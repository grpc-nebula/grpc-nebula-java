/*
 * Copyright 2016 The gRPC Authors
 * Modifications 2019 Orient Securities Co., Ltd.
 * Modifications 2019 BoCloud Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.internal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.enums.LoadBalanceMode;
import com.orientsec.grpc.common.resource.SystemConfig;
import com.orientsec.grpc.common.util.*;
import com.orientsec.grpc.consumer.ConsistentHashArguments;
import com.orientsec.grpc.consumer.ParameterRouterUtil;
import com.orientsec.grpc.consumer.ThreadLocalVariableUtils;
import com.orientsec.grpc.consumer.core.ConsumerServiceRegistry;
import com.orientsec.grpc.consumer.core.ConsumerServiceRegistryFactory;
import com.orientsec.grpc.consumer.internal.ProvidersListener;
import io.grpc.Attributes;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ChannelLogger;
import io.grpc.ChannelLogger.ChannelLogLevel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ClientStreamTracer;
import io.grpc.CompressorRegistry;
import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.Context;
import io.grpc.DecompressorRegistry;
import io.grpc.EquivalentAddressGroup;
import io.grpc.InternalChannelz;
import io.grpc.InternalChannelz.ChannelStats;
import io.grpc.InternalChannelz.ChannelTrace;
import io.grpc.InternalInstrumented;
import io.grpc.InternalLogId;
import io.grpc.InternalWithLogId;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancer.PickResult;
import io.grpc.LoadBalancer.PickSubchannelArgs;
import io.grpc.LoadBalancer.SubchannelPicker;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.SynchronizationContext;
import io.grpc.internal.ClientCallImpl.ClientTransportProvider;
import io.grpc.internal.RetriableStream.ChannelBufferMeter;
import io.grpc.internal.RetriableStream.Throttle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.orientsec.grpc.common.constant.RegistryConstants.CLIENT_REGISTRY_THREAD_NAME;
import static io.grpc.ConnectivityState.IDLE;
import static io.grpc.ConnectivityState.SHUTDOWN;
import static io.grpc.ConnectivityState.TRANSIENT_FAILURE;
import static io.grpc.internal.ServiceConfigInterceptor.HEDGING_POLICY_KEY;
import static io.grpc.internal.ServiceConfigInterceptor.RETRY_POLICY_KEY;

/** A communication channel for making outgoing RPCs. */
@ThreadSafe
public final class ManagedChannelImpl extends ManagedChannel implements
    InternalInstrumented<ChannelStats> {
  static final Logger logger = LoggerFactory.getLogger(ManagedChannelImpl.class);

  // Matching this pattern means the target string is a URI target or at least intended to be one.
  // A URI target must be an absolute hierarchical URI.
  // From RFC 2396: scheme = alpha *( alpha | digit | "+" | "-" | "." )
  @VisibleForTesting
  static final Pattern URI_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*:/.*");

  static final long IDLE_TIMEOUT_MILLIS_DISABLE = -1;

  @VisibleForTesting
  static final long SUBCHANNEL_SHUTDOWN_DELAY_SECONDS = 5;

  @VisibleForTesting
  static final Status SHUTDOWN_NOW_STATUS =
      Status.UNAVAILABLE.withDescription("Channel shutdownNow invoked");

  @VisibleForTesting
  static final Status SHUTDOWN_STATUS =
      Status.UNAVAILABLE.withDescription("Channel shutdown invoked");

  @VisibleForTesting
  static final Status SUBCHANNEL_SHUTDOWN_STATUS =
      Status.UNAVAILABLE.withDescription("Subchannel shutdown invoked");

  private final InternalLogId logId = InternalLogId.allocate(getClass().getName());
  private final String target;
  private final NameResolver.Factory nameResolverFactory;
  private final Attributes nameResolverParams;
  private final LoadBalancer.Factory loadBalancerFactory;
  private final ClientTransportFactory transportFactory;
  private final ScheduledExecutorForBalancer scheduledExecutorForBalancer;
  private final Executor executor;
  private final ObjectPool<? extends Executor> executorPool;
  private final ObjectPool<? extends Executor> balancerRpcExecutorPool;
  private final ExecutorHolder balancerRpcExecutorHolder;
  private final TimeProvider timeProvider;
  private final int maxTraceEvents;

  private final SynchronizationContext syncContext = new SynchronizationContext(
      new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
          logger.error(
              "[" + getLogId() + "] Uncaught exception in the SynchronizationContext. Panic!",
              e);
          panic(e);
        }
      });

  private boolean fullStreamDecompression;

  private final DecompressorRegistry decompressorRegistry;
  private final CompressorRegistry compressorRegistry;

  private final Supplier<Stopwatch> stopwatchSupplier;
  /** The timout before entering idle mode. */
  private final long idleTimeoutMillis;

  private final ConnectivityStateManager channelStateManager = new ConnectivityStateManager();

  private final ServiceConfigInterceptor serviceConfigInterceptor;

  private final BackoffPolicy.Provider backoffPolicyProvider;

  /**
   * We delegate to this channel, so that we can have interceptors as necessary. If there aren't
   * any interceptors and the {@link io.grpc.BinaryLog} is {@code null} then this will just be a
   * {@link RealChannel}.
   */
  private final Channel interceptorChannel;
  @Nullable private final String userAgent;

  // Only null after channel is terminated. Must be assigned from the syncContext.
  private NameResolver nameResolver;

  // Must be accessed from the syncContext.
  private boolean nameResolverStarted;

  // null when channel is in idle mode.  Must be assigned from syncContext.
  @Nullable
  private LbHelperImpl lbHelper;

  // Must ONLY be assigned from updateSubchannelPicker(), which is called from syncContext.
  // null if channel is in idle mode.
  @Nullable
  private volatile SubchannelPicker subchannelPicker;

  // Must be accessed from the syncContext
  private boolean panicMode;

  // Must be mutated from syncContext
  // If any monitoring hook to be added later needs to get a snapshot of this Set, we could
  // switch to a ConcurrentHashMap.
  private final Set<InternalSubchannel> subchannels = new HashSet<InternalSubchannel>(16, .75f);

  // Must be mutated from syncContext
  private final Set<OobChannel> oobChannels = new HashSet<OobChannel>(1, .75f);

  // reprocess() must be run from syncContext
  private final DelayedClientTransport delayedTransport;
  private final UncommittedRetriableStreamsRegistry uncommittedRetriableStreamsRegistry
      = new UncommittedRetriableStreamsRegistry();

  private static final ClientTransport IN_BLACKLIST_TRANSPORT =
          new FailingClientTransport(Status.PERMISSION_DENIED.withDescription("该服务未对当前客户端开放权限，或者该服务不可用！"), ClientStreamListener.RpcProgress.REFUSED);

  private static final ClientTransport NO_PROVIDER_TRANSPORT =
          new FailingClientTransport(Status.UNAVAILABLE.withDescription("注册中心上没有当前客户端调用的服务，请检查调用的服务接口名称是否正确！"), ClientStreamListener.RpcProgress.REFUSED);

  private static final ClientTransport BLOCKED_BY_PARAM_ROUTER_TRANSPORT =
      new FailingClientTransport(Status.CANCELLED.withDescription("该服务由于参数路由限制，无可用提供者，取消本次调用！"), ClientStreamListener.RpcProgress.REFUSED);

  // Shutdown states.
  //
  // Channel's shutdown process:
  // 1. shutdown(): stop accepting new calls from applications
  //   1a shutdown <- true
  //   1b subchannelPicker <- null
  //   1c delayedTransport.shutdown()
  // 2. delayedTransport terminated: stop stream-creation functionality
  //   2a terminating <- true
  //   2b loadBalancer.shutdown()
  //     * LoadBalancer will shutdown subchannels and OOB channels
  //   2c loadBalancer <- null
  //   2d nameResolver.shutdown()
  //   2e nameResolver <- null
  // 3. All subchannels and OOB channels terminated: Channel considered terminated

  private final AtomicBoolean shutdown = new AtomicBoolean(false);
  // Must only be mutated and read from syncContext
  private boolean shutdownNowed;
  // Must be mutated from syncContext
  private volatile boolean terminating;
  // Must be mutated from syncContext
  private volatile boolean terminated;
  private final CountDownLatch terminatedLatch = new CountDownLatch(1);

  // 负载均衡模式集合
  private volatile Map<String, String> loadBalanceModeMap = SystemConfig.getLoadBalanceModeMap();

  private final CallTracer.Factory callTracerFactory;
  private final CallTracer channelCallTracer;
  private final ChannelTracer channelTracer;
  private final ChannelLogger channelLogger;
  private final InternalChannelz channelz;
  @CheckForNull
  private Boolean haveBackends; // a flag for doing channel tracing when flipped
  @Nullable
  private Map<String, Object> lastServiceConfig; // used for channel tracing when value changed

  // One instance per channel.
  private final ChannelBufferMeter channelBufferUsed = new ChannelBufferMeter();

  @Nullable
  private Throttle throttle;

  private final long perRpcBufferLimit;
  private final long channelBufferLimit;

  // Temporary false flag that can skip the retry code path.
  private final boolean retryEnabled;

  /** 上一次切换连接时间 */
  private volatile long lastSwitchConnMillisecond;

  /** 配置的连接切换的间隔时间 */
  private static final long configSwitchConnMillisecond = initSwitchMillisecond();

  // Called from syncContext
  private final ManagedClientTransport.Listener delayedTransportListener =
      new DelayedTransportListener();

  /**
   * 初始化connection模式切换时间
   *
   * @return
   * @author wlh
   * @since 2019/12/16
   */
  private static long initSwitchMillisecond(){
    String key = GlobalConstants.Consumer.Key.LOADBALANCE_CONNECTION_SWITCHTIME;
    Properties properties = SystemConfig.getProperties();

    // 默认缺省10分钟
    int defaultTime = 10;

    int configTime = PropertiesUtils.getValidIntegerValue(properties, key, defaultTime);

    if (configTime <= 0) {
      configTime = defaultTime;
    }
    // 将配置的分钟数转为毫秒值
    long resultMillisecond = configTime * 60L * 1000L;

    logger.info(key + " = " + resultMillisecond);

    return resultMillisecond;
  }

  // Must be called from syncContext
  private void maybeShutdownNowSubchannels() {
    if (shutdownNowed) {
      for (InternalSubchannel subchannel : subchannels) {
        subchannel.shutdownNow(SHUTDOWN_NOW_STATUS);
      }
      for (OobChannel oobChannel : oobChannels) {
        oobChannel.getInternalSubchannel().shutdownNow(SHUTDOWN_NOW_STATUS);
      }
    }
  }

  // Must be accessed from syncContext
  @VisibleForTesting
  final InUseStateAggregator<Object> inUseStateAggregator = new IdleModeStateAggregator();

  @Override
  public ListenableFuture<ChannelStats> getStats() {
    final SettableFuture<ChannelStats> ret = SettableFuture.create();
    final class StatsFetcher implements Runnable {
      @Override
      public void run() {
        ChannelStats.Builder builder = new ChannelStats.Builder();
        channelCallTracer.updateBuilder(builder);
        channelTracer.updateBuilder(builder);
        builder.setTarget(target).setState(channelStateManager.getState());
        List<InternalWithLogId> children = new ArrayList<>();
        children.addAll(subchannels);
        children.addAll(oobChannels);
        builder.setSubchannels(children);
        ret.set(builder.build());
      }
    }

    // subchannels and oobchannels can only be accessed from syncContext
    syncContext.execute(new StatsFetcher());
    return ret;
  }

  @Override
  public InternalLogId getLogId() {
    return logId;
  }

  // Run from syncContext
  private class IdleModeTimer implements Runnable {

    @Override
    public void run() {
      enterIdleMode();
    }
  }

  // Must be called from syncContext
  private void shutdownNameResolverAndLoadBalancer(boolean verifyActive) {
    if (verifyActive) {
      checkState(nameResolver != null, "nameResolver is null");
      checkState(lbHelper != null, "lbHelper is null");
    }
    if (nameResolver != null) {
      cancelNameResolverBackoff();
      nameResolver.shutdown();
      nameResolver = null;
      nameResolverStarted = false;
    }
    if (lbHelper != null) {
      lbHelper.lb.shutdown();
      lbHelper = null;
    }
    subchannelPicker = null;
  }

  /**
   * Make the channel exit idle mode, if it's in it.
   *
   * <p>Must be called from syncContext
   */
  @VisibleForTesting
  void exitIdleMode(final Object argument) {
    if (shutdown.get() || panicMode) {
      return;
    }
    if (inUseStateAggregator.isInUse()) {
      // Cancel the timer now, so that a racing due timer will not put Channel on idleness
      // when the caller of exitIdleMode() is about to use the returned loadBalancer.
      cancelIdleTimer(false);
    } else {
      // exitIdleMode() may be called outside of inUseStateAggregator.handleNotInUse() while
      // isInUse() == false, in which case we still need to schedule the timer.
      rescheduleIdleTimer();
    }
    if (lbHelper != null) {
      return;
    }
    channelLogger.log(ChannelLogLevel.INFO, "Exiting idle mode");
    LbHelperImpl lbHelper = new LbHelperImpl(nameResolver);
    lbHelper.lb = loadBalancerFactory.newLoadBalancer(lbHelper);
    // Delay setting lbHelper until fully initialized, since loadBalancerFactory is user code and
    // may throw. We don't want to confuse our state, even if we will enter panic mode.
    this.lbHelper = lbHelper;

    NameResolverListenerImpl listener = new NameResolverListenerImpl(lbHelper, argument);
    try {
      nameResolver.start(listener);
      nameResolverStarted = true;
    } catch (Throwable t) {
      listener.onError(Status.fromThrowable(t));
    }
  }

  // Must be run from syncContext
  private void enterIdleMode() {
    // nameResolver and loadBalancer are guaranteed to be non-null.  If any of them were null,
    // either the idleModeTimer ran twice without exiting the idle mode, or the task in shutdown()
    // did not cancel idleModeTimer, or enterIdle() ran while shutdown or in idle, all of
    // which are bugs.
    shutdownNameResolverAndLoadBalancer(true);
    delayedTransport.reprocess(null);
    nameResolver = getNameResolver(target, nameResolverFactory, nameResolverParams);

    channelLogger.log(ChannelLogLevel.INFO, "Entering IDLE state");
    channelStateManager.gotoState(IDLE);
    if (inUseStateAggregator.isInUse()) {
      //----begin----获取一致性Hash的参数值----
      final Object argument = getArgument();
      //----end------获取一致性Hash的参数值----

      exitIdleMode(argument);
    }

    //----begin----超时后对新创建的nameResolver需要增加一个额外的操作----
    if (consumerServiceRegistry == null) {
      consumerServiceRegistry = ConsumerServiceRegistryFactory.getRegistry();
    }
    nameResolver.setRegistry(consumerServiceRegistry);
    nameResolver.setManagedChannel(this);
    new Thread(registryRunnable, CLIENT_REGISTRY_THREAD_NAME).start();
    //----end----超时后对新创建的nameResolver需要增加一个额外的操作----
  }

  // Must be run from syncContext
  private void cancelIdleTimer(boolean permanent) {
    idleTimer.cancel(permanent);
  }

  // Always run from syncContext
  private void rescheduleIdleTimer() {
    if (idleTimeoutMillis == IDLE_TIMEOUT_MILLIS_DISABLE) {
      return;
    }
    idleTimer.reschedule(idleTimeoutMillis, TimeUnit.MILLISECONDS);
  }

  // Run from syncContext
  @VisibleForTesting
  class NameResolverRefresh implements Runnable {
    // Only mutated from syncContext
    boolean cancelled;

    @Override
    public void run() {
      if (cancelled) {
        // Race detected: this task was scheduled on syncContext before
        // cancelNameResolverBackoff() could cancel the timer.
        return;
      }
      nameResolverRefreshFuture = null;
      nameResolverRefresh = null;
      if (nameResolver != null) {
        nameResolver.refresh();
      }
    }
  }

  // Must be used from syncContext
  @Nullable private ScheduledFuture<?> nameResolverRefreshFuture;
  // Must be used from syncContext
  @Nullable private NameResolverRefresh nameResolverRefresh;
  // The policy to control backoff between name resolution attempts. Non-null when an attempt is
  // scheduled. Must be used from syncContext
  @Nullable private BackoffPolicy nameResolverBackoffPolicy;

  // Must be run from syncContext
  private void cancelNameResolverBackoff() {
    if (nameResolverRefreshFuture != null) {
      nameResolverRefreshFuture.cancel(false);
      nameResolverRefresh.cancelled = true;
      nameResolverRefreshFuture = null;
      nameResolverRefresh = null;
      nameResolverBackoffPolicy = null;
    }
  }

  private final class ChannelTransportProvider implements ClientTransportProvider {
    private volatile EquivalentAddressGroup previousAddressGroup = null;

    @Override
    public ClientTransport get(PickSubchannelArgs args) {
      SubchannelPicker pickerCopy = subchannelPicker;
      if (shutdown.get()) {
        // If channel is shut down, delayedTransport is also shut down which will fail the stream
        // properly.
        return delayedTransport;
      }

      //----begin----获取一致性Hash的参数值----
      final Object argument = getArgument();
      //----end------获取一致性Hash的参数值----

      String lbMode = "";

      if (pickerCopy == null) {
        final class ExitIdleModeForTransport implements Runnable {
          @Override
          public void run() {
            exitIdleMode(argument);
          }
        }

        syncContext.execute(new ExitIdleModeForTransport());
        return delayedTransport;
      } else {
        lbMode = getloadBalanceMode(nameResolver);
        boolean connectionOutOfTime = false;
        if (LoadBalanceMode.connection.name().equals(lbMode)) {
          if (lastSwitchConnMillisecond == 0) {
            lastSwitchConnMillisecond = System.currentTimeMillis();
          } else {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - lastSwitchConnMillisecond >= configSwitchConnMillisecond) {
              connectionOutOfTime = true;
              lastSwitchConnMillisecond = currentTimeMillis;
              nameResolver.getLastProviderMapAfterParamRoute().clear();
            }
          }
        }

        //----begin----参数路由重选提供者----

        String method = getMethod(nameResolver);
        boolean isParameterRouterResolved = reselectServerByRouter(method, args.getCallOptions());
        if (isParameterRouterResolved &&
            nameResolver.getServiceProviderMap() != null && nameResolver.getServiceProviderMap().isEmpty()) {
          return BLOCKED_BY_PARAM_ROUTER_TRANSPORT;
        }
        //----end----参数路由重选提供者----

        //----begin----请求负载均衡----

        // 如果负载均衡模式为“请求负载均衡”，每次都触发负载均衡算法 如果已经由参数路由选择过提供者，则不触发
        if (LoadBalanceMode.request.name().equals(lbMode)) {
          if (!isParameterRouterResolved) {
            nameResolver.resolveServerInfo(argument, method);
          }
          pickerCopy = subchannelPicker;// 切换服务器会导致subchannelPicker发生变化
        } else {
          if (connectionOutOfTime) {
            if (!isParameterRouterResolved) {
              nameResolver.resolveServerInfo(argument, method);
            }
            pickerCopy = subchannelPicker;// 切换服务器会导致subchannelPicker发生变化
          }
        }

        //----end----请求负载均衡----
      }
      // There is no need to reschedule the idle timer here.
      //
      // pickerCopy != null, which means idle timer has not expired when this method starts.
      // Even if idle timer expires right after we grab pickerCopy, and it shuts down LoadBalancer
      // which calls Subchannel.shutdown(), the InternalSubchannel will be actually shutdown after
      // SUBCHANNEL_SHUTDOWN_DELAY_SECONDS, which gives the caller time to start RPC on it.
      //
      // In most cases the idle timer is scheduled to fire after the transport has created the
      // stream, which would have reported in-use state to the channel that would have cancelled
      // the idle timer.
      PickResult pickResult = pickerCopy.pickSubchannel(args);
      ClientTransport transport = GrpcUtil.getTransportFromPickResult(
          pickResult, args.getCallOptions().isWaitForReady());

      //----begin----检查服务提供者是否存在----

      if (nameResolver.getProvidersCount() == 0) {
        /*
         * 当服务端与zookeeper断开连接、服务注册信息丢失后，如果客户端与服务端连接正常，那么
         * 客户端与服务端依然可以正常通信。
         */
        if (transport == null || (transport instanceof FailingClientTransport)) {
          transport = getTransportFromLb(args, true);
        }

        InetSocketAddress inetSocketAddress = null;

        if (transport instanceof InternalSubchannel.CallTracingTransport) {
          InternalSubchannel.CallTracingTransport callTransport;
          callTransport = (InternalSubchannel.CallTracingTransport) transport;
          ConnectionClientTransport connTransport = callTransport.delegate();
          SocketAddress socketAddress = connTransport.getAddress();
          if (socketAddress instanceof InetSocketAddress) {
            inetSocketAddress = (InetSocketAddress) socketAddress;
          }
        }

        ProvidersListener listener = nameResolver.getProvidersListener();

        if (inetSocketAddress == null) {
          if (listener != null && listener.isProviderListEmpty()) {
            return NO_PROVIDER_TRANSPORT;
          } else {
            return IN_BLACKLIST_TRANSPORT;
          }
        }

        InetAddress inetAddress = inetSocketAddress.getAddress();
        String host = inetAddress.getHostAddress();
        int port = inetSocketAddress.getPort();

        if (StringUtils.isNotEmpty(host)) {
          // 如果transport的地址在被路由规则过滤的服务端的集合中，那么仍然需要报错
          boolean filtered = nameResolver.isInfilteredProviders(host, port);

          if (filtered) {
            if (listener != null && listener.isProviderListEmpty()) {
              return NO_PROVIDER_TRANSPORT;
            } else {
              return IN_BLACKLIST_TRANSPORT;
            }
          }

          // 该服务端的分组是否满足客户端的要求
          boolean valid = nameResolver.isGroupValid(host, port);
          if (!valid) {
            if (listener != null && listener.isProviderListEmpty()) {
              return NO_PROVIDER_TRANSPORT;
            } else {
              return IN_BLACKLIST_TRANSPORT;
            }
          }
        }
      } else {
        // 当客户端与服务端连接没建立好的时候，等待连接创建成功
        boolean invalid = (transport == null || (transport instanceof FailingClientTransport));
        if (invalid && LoadBalanceMode.request.name().equals(lbMode)) {
          transport = getTransportFromLb(args, false);
        }
      }

      //----end------检查服务提供者是否存在----

      if (transport != null) {
        return transport;
      }
      return delayedTransport;
    }

    /**
     * 从负载均衡对象中获取transport
     *
     * @author sxp
     * @since 2019/11/14
     */
    private ClientTransport getTransportFromLb(PickSubchannelArgs args, boolean doResolve) {
      LoadBalancer lb = getLoadBalancer();
      if (lb == null) {
        return null;
      }

      if (doResolve) {
        EquivalentAddressGroup server = lb.getAddresses();
        if (server == null) {
          return null;
        }

        boolean success = nameResolver.resolveOneServer(server);
        if (!success) {
          return null;
        }
      }

      SubchannelPicker pickerCopy;
      ClientTransport transport = null;
      PickResult pickResult;

      int retryTimes = 100;
      for (int i = 0; i < retryTimes; i++) {
        pickerCopy = subchannelPicker;
        pickResult = pickerCopy.pickSubchannel(args);
        transport = GrpcUtil.getTransportFromPickResult(
                pickResult, args.getCallOptions().isWaitForReady());
        if (transport == null || (transport instanceof FailingClientTransport)) {
          if (i % 20 == 0) {
            logger.info("wait for transport to be created...");
          }
          ThreadUtils.sleepQuietly(TimeUnit.MILLISECONDS, 10);
        } else {
          break;
        }
      }

      return transport;
    }

    /**
     * 依据参数路由重选服务提供者
     * 被筛选的服务提供者列表为ZookeeperNameResolver中的ProviderMap
     *
     * @since nebula-1.2.8 2020-12-07
     * @author zhuyujie
     */
    private boolean reselectServerByRouter(String method, CallOptions callOptions) {
      Map<String, Object> routerMap = callOptions.getOption(GrpcUtil.ROUTER_MAP_KEY);
      if (!ParameterRouterUtil.isEnabled()) {
        if (routerMap != null) {
          logger.info("未开启参数路由功能，本次调用的参数路由将不会生效！");
        }
        return false;
      }

      if (routerMap == null) {
        routerMap = new HashMap<>();
      }
      routerMap.put("method", method);
      // 重新选择服务提供者
      return nameResolver.reselectProviderByParameterRouter(routerMap, method, getArgument());
    }

    @Override
    public <ReqT> RetriableStream<ReqT> newRetriableStream(
        final MethodDescriptor<ReqT, ?> method,
        final CallOptions callOptions,
        final Metadata headers,
        final Context context) {
      checkState(retryEnabled, "retry should be enabled");
      final class RetryStream extends RetriableStream<ReqT> {
        RetryStream() {
          super(
              method,
              headers,
              channelBufferUsed,
              perRpcBufferLimit,
              channelBufferLimit,
              getCallExecutor(callOptions),
              transportFactory.getScheduledExecutorService(),
              callOptions.getOption(RETRY_POLICY_KEY),
              callOptions.getOption(HEDGING_POLICY_KEY),
              throttle);
        }

        @Override
        Status prestart() {
          return uncommittedRetriableStreamsRegistry.add(this);
        }

        @Override
        void postCommit() {
          uncommittedRetriableStreamsRegistry.remove(this);
        }

        @Override
        ClientStream newSubstream(ClientStreamTracer.Factory tracerFactory, Metadata newHeaders) {
          CallOptions newOptions = callOptions.withStreamTracerFactory(tracerFactory);
          ClientTransport transport =
              get(new PickSubchannelArgsImpl(method, newHeaders, newOptions));
          Context origContext = context.attach();
          try {
            return transport.newStream(method, newHeaders, newOptions);
          } finally {
            context.detach(origContext);
          }
        }
      }

      return new RetryStream();
    }
  }

  private final ClientTransportProvider transportProvider = new ChannelTransportProvider();

  private final Rescheduler idleTimer;

  //----begin----定义客户端注册对象----

  private ConsumerServiceRegistry consumerServiceRegistry = ConsumerServiceRegistryFactory.getRegistry();

  //----end----定义客户端注册对象----

  ManagedChannelImpl(
      AbstractManagedChannelImplBuilder<?> builder,
      ClientTransportFactory clientTransportFactory,
      BackoffPolicy.Provider backoffPolicyProvider,
      ObjectPool<? extends Executor> balancerRpcExecutorPool,
      Supplier<Stopwatch> stopwatchSupplier,
      List<ClientInterceptor> interceptors,
      final TimeProvider timeProvider) {
    this.target = checkNotNull(builder.target, "target");
    this.nameResolverFactory = builder.getNameResolverFactory();
    this.nameResolverParams = checkNotNull(builder.getNameResolverParams(), "nameResolverParams");
    this.nameResolver = getNameResolver(target, nameResolverFactory, nameResolverParams);

    //----begin----注册Consumer信息，设置注册对象----

    this.nameResolver.setRegistry(consumerServiceRegistry);
    this.nameResolver.setManagedChannel(this);

    //----end----注册Consumer信息，设置注册对象----

    this.timeProvider = checkNotNull(timeProvider, "timeProvider");
    maxTraceEvents = builder.maxTraceEvents;
    channelTracer = new ChannelTracer(
        logId, builder.maxTraceEvents, timeProvider.currentTimeNanos(),
        "Channel for '" + target + "'");
    channelLogger = new ChannelLoggerImpl(channelTracer, timeProvider);
    if (builder.loadBalancerFactory == null) {
      this.loadBalancerFactory = new AutoConfiguredLoadBalancerFactory();
    } else {
      this.loadBalancerFactory = builder.loadBalancerFactory;
    }
    this.executorPool = checkNotNull(builder.executorPool, "executorPool");
    this.balancerRpcExecutorPool = checkNotNull(balancerRpcExecutorPool, "balancerRpcExecutorPool");
    this.balancerRpcExecutorHolder = new ExecutorHolder(balancerRpcExecutorPool);
    this.executor = checkNotNull(executorPool.getObject(), "executor");
    this.delayedTransport = new DelayedClientTransport(this.executor, this.syncContext);
    this.delayedTransport.start(delayedTransportListener);
    this.backoffPolicyProvider = backoffPolicyProvider;
    this.transportFactory =
        new CallCredentialsApplyingTransportFactory(clientTransportFactory, this.executor);
    this.scheduledExecutorForBalancer =
        new ScheduledExecutorForBalancer(transportFactory.getScheduledExecutorService());
    this.retryEnabled = builder.retryEnabled && !builder.temporarilyDisableRetry;
    serviceConfigInterceptor = new ServiceConfigInterceptor(
        retryEnabled, builder.maxRetryAttempts, builder.maxHedgedAttempts);
    Channel channel = new RealChannel(nameResolver.getServiceAuthority());
    channel = ClientInterceptors.intercept(channel, serviceConfigInterceptor);
    if (builder.binlog != null) {
      channel = builder.binlog.wrapChannel(channel);
    }
    this.interceptorChannel = ClientInterceptors.intercept(channel, interceptors);
    this.stopwatchSupplier = checkNotNull(stopwatchSupplier, "stopwatchSupplier");
    if (builder.idleTimeoutMillis == IDLE_TIMEOUT_MILLIS_DISABLE) {
      this.idleTimeoutMillis = builder.idleTimeoutMillis;
    } else {
      checkArgument(
          builder.idleTimeoutMillis
              >= AbstractManagedChannelImplBuilder.IDLE_MODE_MIN_TIMEOUT_MILLIS,
          "invalid idleTimeoutMillis %s", builder.idleTimeoutMillis);
      this.idleTimeoutMillis = builder.idleTimeoutMillis;
    }

    idleTimer = new Rescheduler(
        new IdleModeTimer(),
        syncContext,
        transportFactory.getScheduledExecutorService(),
        stopwatchSupplier.get());
    this.fullStreamDecompression = builder.fullStreamDecompression;
    this.decompressorRegistry = checkNotNull(builder.decompressorRegistry, "decompressorRegistry");
    this.compressorRegistry = checkNotNull(builder.compressorRegistry, "compressorRegistry");
    this.userAgent = builder.userAgent;

    this.channelBufferLimit = builder.retryBufferSize;
    this.perRpcBufferLimit = builder.perRpcBufferLimit;
    final class ChannelCallTracerFactory implements CallTracer.Factory {
      @Override
      public CallTracer create() {
        return new CallTracer(timeProvider);
      }
    }

    this.callTracerFactory = new ChannelCallTracerFactory();
    channelCallTracer = callTracerFactory.create();
    this.channelz = checkNotNull(builder.channelz);
    channelz.addRootChannel(this);


    //----begin----注册Consumer信息，调用注册方法----

    new Thread(registryRunnable, CLIENT_REGISTRY_THREAD_NAME).start();

    //----end----功能描述，调用注册方法----
  }

  private Runnable registryRunnable = new Runnable() {
    @Override
    public void run() {
      nameResolver = nameResolver.build();
      nameResolver.registry();
    }
  };

  @VisibleForTesting
  static NameResolver getNameResolver(String target, NameResolver.Factory nameResolverFactory,
      Attributes nameResolverParams) {
    // Finding a NameResolver. Try using the target string as the URI. If that fails, try prepending
    // "dns:///".
    URI targetUri = null;
    StringBuilder uriSyntaxErrors = new StringBuilder();
    try {
      targetUri = new URI(target);
      // For "localhost:8080" this would likely cause newNameResolver to return null, because
      // "localhost" is parsed as the scheme. Will fall into the next branch and try
      // "dns:///localhost:8080".
    } catch (URISyntaxException e) {
      // Can happen with ip addresses like "[::1]:1234" or 127.0.0.1:1234.
      uriSyntaxErrors.append(e.getMessage());
    }
    if (targetUri != null) {
      NameResolver resolver = nameResolverFactory.newNameResolver(targetUri, nameResolverParams);
      if (resolver != null) {
        return resolver;
      }
      // "foo.googleapis.com:8080" cause resolver to be null, because "foo.googleapis.com" is an
      // unmapped scheme. Just fall through and will try "dns:///foo.googleapis.com:8080"
    }

    // If we reached here, the targetUri couldn't be used.
    if (!URI_PATTERN.matcher(target).matches()) {
      // It doesn't look like a URI target. Maybe it's an authority string. Try with the default
      // scheme from the factory.
      try {
        targetUri = new URI(nameResolverFactory.getDefaultScheme(), "", "/" + target, null);
      } catch (URISyntaxException e) {
        // Should not be possible.
        throw new IllegalArgumentException(e);
      }
      NameResolver resolver = nameResolverFactory.newNameResolver(targetUri, nameResolverParams);
      if (resolver != null) {
        return resolver;
      }
    }
    throw new IllegalArgumentException(String.format(
        "cannot find a NameResolver for %s%s",
        target, uriSyntaxErrors.length() > 0 ? " (" + uriSyntaxErrors + ")" : ""));
  }

  /**
   * Initiates an orderly shutdown in which preexisting calls continue but new calls are immediately
   * cancelled.
   */
  @Override
  public ManagedChannelImpl shutdown() {
    channelLogger.log(ChannelLogLevel.DEBUG, "shutdown() called");
    if (!shutdown.compareAndSet(false, true)) {
      return this;
    }

    // Put gotoState(SHUTDOWN) as early into the syncContext's queue as possible.
    // delayedTransport.shutdown() may also add some tasks into the queue. But some things inside
    // delayedTransport.shutdown() like setting delayedTransport.shutdown = true are not run in the
    // syncContext's queue and should not be blocked, so we do not drain() immediately here.
    final class Shutdown implements Runnable {
      @Override
      public void run() {
        channelLogger.log(ChannelLogLevel.INFO, "Entering SHUTDOWN state");
        channelStateManager.gotoState(SHUTDOWN);
      }
    }

    syncContext.executeLater(new Shutdown());

    uncommittedRetriableStreamsRegistry.onShutdown(SHUTDOWN_STATUS);
    final class CancelIdleTimer implements Runnable {
      @Override
      public void run() {
        cancelIdleTimer(/* permanent= */ true);
      }
    }

    syncContext.execute(new CancelIdleTimer());
    return this;
  }

  /**
   * Initiates a forceful shutdown in which preexisting and new calls are cancelled. Although
   * forceful, the shutdown process is still not instantaneous; {@link #isTerminated()} will likely
   * return {@code false} immediately after this method returns.
   */
  @Override
  public ManagedChannelImpl shutdownNow() {
    channelLogger.log(ChannelLogLevel.DEBUG, "shutdownNow() called");
    shutdown();
    uncommittedRetriableStreamsRegistry.onShutdownNow(SHUTDOWN_NOW_STATUS);
    final class ShutdownNow implements Runnable {
      @Override
      public void run() {
        if (shutdownNowed) {
          return;
        }
        shutdownNowed = true;
        maybeShutdownNowSubchannels();
      }
    }

    syncContext.execute(new ShutdownNow());
    return this;
  }

  // Called from syncContext
  @VisibleForTesting
  void panic(final Throwable t) {
    if (panicMode) {
      // Preserve the first panic information
      return;
    }
    panicMode = true;
    cancelIdleTimer(/* permanent= */ true);
    shutdownNameResolverAndLoadBalancer(false);
    final class PanicSubchannelPicker extends SubchannelPicker {
      private final PickResult panicPickResult =
          PickResult.withDrop(
              Status.INTERNAL.withDescription("Panic! This is a bug!").withCause(t));

      @Override
      public PickResult pickSubchannel(PickSubchannelArgs args) {
        return panicPickResult;
      }
    }

    updateSubchannelPicker(new PanicSubchannelPicker());
    channelLogger.log(ChannelLogLevel.ERROR, "PANIC! Entering TRANSIENT_FAILURE");
    channelStateManager.gotoState(TRANSIENT_FAILURE);
  }

  // Called from syncContext
  private void updateSubchannelPicker(SubchannelPicker newPicker) {
    subchannelPicker = newPicker;
    delayedTransport.reprocess(newPicker);
  }

  @Override
  public boolean isShutdown() {
    return shutdown.get();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return terminatedLatch.await(timeout, unit);
  }

  @Override
  public boolean isTerminated() {
    return terminated;
  }

  /*
   * Creates a new outgoing call on the channel.
   */
  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(MethodDescriptor<ReqT, RespT> method,
      CallOptions callOptions) {
    return interceptorChannel.newCall(method, callOptions);
  }

  @Override
  public String authority() {
    return interceptorChannel.authority();
  }

  private Executor getCallExecutor(CallOptions callOptions) {
    Executor executor = callOptions.getExecutor();
    if (executor == null) {
      executor = this.executor;
    }
    return executor;
  }

  private class RealChannel extends Channel {
    // Set when the NameResolver is initially created. When we create a new NameResolver for the
    // same target, the new instance must have the same value.
    private final String authority;

    private RealChannel(String authority) {
      this.authority =  checkNotNull(authority, "authority");
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(MethodDescriptor<ReqT, RespT> method,
        CallOptions callOptions) {
      return new ClientCallImpl<ReqT, RespT>(
              method,
              getCallExecutor(callOptions),
              callOptions,
              transportProvider,
              terminated ? null : transportFactory.getScheduledExecutorService(),
              channelCallTracer,
              retryEnabled)
          .setFullStreamDecompression(fullStreamDecompression)
          .setDecompressorRegistry(decompressorRegistry)
          .setCompressorRegistry(compressorRegistry);
    }

    @Override
    public String authority() {
      return authority;
    }
  }

  /**
   * Terminate the channel if termination conditions are met.
   */
  // Must be run from syncContext
  private void maybeTerminateChannel() {
    if (terminated) {
      return;
    }
    if (shutdown.get() && subchannels.isEmpty() && oobChannels.isEmpty()) {
      channelLogger.log(ChannelLogLevel.INFO, "Terminated");
      channelz.removeRootChannel(this);
      terminated = true;
      terminatedLatch.countDown();
      executorPool.returnObject(executor);
      balancerRpcExecutorHolder.release();
      // Release the transport factory so that it can deallocate any resources.
      transportFactory.close();
    }
  }

  @Override
  public ConnectivityState getState(boolean requestConnection) {
    ConnectivityState savedChannelState = channelStateManager.getState();
    if (requestConnection && savedChannelState == IDLE) {
      final class RequestConnection implements Runnable {
        @Override
        public void run() {
          //----begin----获取一致性Hash的参数值----
          final Object argument = getArgument();
          //----end------获取一致性Hash的参数值----

          exitIdleMode(argument);
          if (subchannelPicker != null) {
            subchannelPicker.requestConnection();
          }
        }
      }

      syncContext.execute(new RequestConnection());
    }
    return savedChannelState;
  }

  @Override
  public void notifyWhenStateChanged(final ConnectivityState source, final Runnable callback) {
    final class NotifyStateChanged implements Runnable {
      @Override
      public void run() {
        channelStateManager.notifyWhenStateChanged(callback, executor, source);
      }
    }

    syncContext.execute(new NotifyStateChanged());
  }

  @Override
  public void resetConnectBackoff() {
    final class ResetConnectBackoff implements Runnable {
      @Override
      public void run() {
        if (shutdown.get()) {
          return;
        }
        if (nameResolverRefreshFuture != null) {
          checkState(nameResolverStarted, "name resolver must be started");
          cancelNameResolverBackoff();
          nameResolver.refresh();
        }
        for (InternalSubchannel subchannel : subchannels) {
          subchannel.resetConnectBackoff();
        }
        for (OobChannel oobChannel : oobChannels) {
          oobChannel.resetConnectBackoff();
        }
      }
    }

    syncContext.execute(new ResetConnectBackoff());
  }

  @Override
  public void enterIdle() {
    final class PrepareToLoseNetworkRunnable implements Runnable {
      @Override
      public void run() {
        if (shutdown.get() || lbHelper == null) {
          return;
        }
        cancelIdleTimer(/* permanent= */ false);
        enterIdleMode();
      }
    }

    syncContext.execute(new PrepareToLoseNetworkRunnable());
  }

  /**
   * A registry that prevents channel shutdown from killing existing retry attempts that are in
   * backoff.
   */
  private final class UncommittedRetriableStreamsRegistry {
    // TODO(zdapeng): This means we would acquire a lock for each new retry-able stream,
    // it's worthwhile to look for a lock-free approach.
    final Object lock = new Object();

    @GuardedBy("lock")
    Collection<ClientStream> uncommittedRetriableStreams = new HashSet<>();

    @GuardedBy("lock")
    Status shutdownStatus;

    void onShutdown(Status reason) {
      boolean shouldShutdownDelayedTransport = false;
      synchronized (lock) {
        if (shutdownStatus != null) {
          return;
        }
        shutdownStatus = reason;
        // Keep the delayedTransport open until there is no more uncommitted streams, b/c those
        // retriable streams, which may be in backoff and not using any transport, are already
        // started RPCs.
        if (uncommittedRetriableStreams.isEmpty()) {
          shouldShutdownDelayedTransport = true;
        }
      }

      if (shouldShutdownDelayedTransport) {
        delayedTransport.shutdown(reason);
      }
    }

    void onShutdownNow(Status reason) {
      onShutdown(reason);
      Collection<ClientStream> streams;

      synchronized (lock) {
        streams = new ArrayList<>(uncommittedRetriableStreams);
      }

      for (ClientStream stream : streams) {
        stream.cancel(reason);
      }
      delayedTransport.shutdownNow(reason);
    }

    /**
     * Registers a RetriableStream and return null if not shutdown, otherwise just returns the
     * shutdown Status.
     */
    @Nullable
    Status add(RetriableStream<?> retriableStream) {
      synchronized (lock) {
        if (shutdownStatus != null) {
          return shutdownStatus;
        }
        uncommittedRetriableStreams.add(retriableStream);
        return null;
      }
    }

    void remove(RetriableStream<?> retriableStream) {
      Status shutdownStatusCopy = null;

      synchronized (lock) {
        uncommittedRetriableStreams.remove(retriableStream);
        if (uncommittedRetriableStreams.isEmpty()) {
          shutdownStatusCopy = shutdownStatus;
          // Because retriable transport is long-lived, we take this opportunity to down-size the
          // hashmap.
          uncommittedRetriableStreams = new HashSet<>();
        }
      }

      if (shutdownStatusCopy != null) {
        delayedTransport.shutdown(shutdownStatusCopy);
      }
    }
  }

  private class LbHelperImpl extends LoadBalancer.Helper {
    LoadBalancer lb;
    final NameResolver nr;

    LbHelperImpl(NameResolver nr) {
      this.nr = checkNotNull(nr, "NameResolver");
    }

    // Must be called from syncContext
    private void handleInternalSubchannelState(ConnectivityStateInfo newState) {
      if (newState.getState() == TRANSIENT_FAILURE || newState.getState() == IDLE) {
        nr.refresh();
      }
    }

    @Override
    public AbstractSubchannel createSubchannel(
        List<EquivalentAddressGroup> addressGroups, Attributes attrs) {
      try {
        syncContext.throwIfNotInThisSynchronizationContext();
      } catch (IllegalStateException e) {
        logger.warn(
            "We sugguest you call createSubchannel() from SynchronizationContext."
            + " Otherwise, it may race with handleSubchannelState()."
            + " See https://github.com/grpc/grpc-java/issues/5015", e);
      }
      checkNotNull(addressGroups, "addressGroups");
      checkNotNull(attrs, "attrs");
      // TODO(ejona): can we be even stricter? Like loadBalancer == null?
      checkState(!terminated, "Channel is terminated");
      final SubchannelImpl subchannel = new SubchannelImpl(attrs);
      long subchannelCreationTime = timeProvider.currentTimeNanos();
      InternalLogId subchannelLogId = InternalLogId.allocate("Subchannel");
      ChannelTracer subchannelTracer =
          new ChannelTracer(
              subchannelLogId, maxTraceEvents, subchannelCreationTime,
              "Subchannel for " + addressGroups);

      final class ManagedInternalSubchannelCallback extends InternalSubchannel.Callback {
        // All callbacks are run in syncContext
        @Override
        void onTerminated(InternalSubchannel is) {
          subchannels.remove(is);
          channelz.removeSubchannel(is);
          maybeTerminateChannel();
        }

        @Override
        void onStateChange(InternalSubchannel is, ConnectivityStateInfo newState) {
          handleInternalSubchannelState(newState);
          // Call LB only if it's not shutdown.  If LB is shutdown, lbHelper won't match.
          if (LbHelperImpl.this == ManagedChannelImpl.this.lbHelper) {
            lb.handleSubchannelState(subchannel, newState);
          }
        }

        @Override
        void onInUse(InternalSubchannel is) {
          inUseStateAggregator.updateObjectInUse(is, true);
        }

        @Override
        void onNotInUse(InternalSubchannel is) {
          inUseStateAggregator.updateObjectInUse(is, false);
        }
      }

      final InternalSubchannel internalSubchannel = new InternalSubchannel(
          addressGroups,
          authority(),
          userAgent,
          backoffPolicyProvider,
          transportFactory,
          transportFactory.getScheduledExecutorService(),
          stopwatchSupplier,
          syncContext,
          new ManagedInternalSubchannelCallback(),
          channelz,
          callTracerFactory.create(),
          subchannelTracer,
          subchannelLogId,
          timeProvider);
      channelTracer.reportEvent(new ChannelTrace.Event.Builder()
          .setDescription("Child Subchannel created")
          .setSeverity(ChannelTrace.Event.Severity.CT_INFO)
          .setTimestampNanos(subchannelCreationTime)
          .setSubchannelRef(internalSubchannel)
          .build());
      channelz.addSubchannel(internalSubchannel);
      subchannel.subchannel = internalSubchannel;

      final class AddSubchannel implements Runnable {
        @Override
        public void run() {
          if (terminating) {
            // Because SynchronizationContext doesn't guarantee the runnable has been executed upon
            // when returning, the subchannel may still be returned to the balancer without being
            // shutdown even if "terminating" is already true.  The subchannel will not be used in
            // this case, because delayed transport has terminated when "terminating" becomes true,
            // and no more requests will be sent to balancer beyond this point.
            internalSubchannel.shutdown(SHUTDOWN_STATUS);
          }
          if (!terminated) {
            // If channel has not terminated, it will track the subchannel and block termination
            // for it.
            subchannels.add(internalSubchannel);
          }
        }
      }

      syncContext.execute(new AddSubchannel());
      return subchannel;
    }

    @Override
    public void updateBalancingState(
        final ConnectivityState newState, final SubchannelPicker newPicker) {
      checkNotNull(newState, "newState");
      checkNotNull(newPicker, "newPicker");
      final class UpdateBalancingState implements Runnable {
        @Override
        public void run() {
          if (LbHelperImpl.this != lbHelper) {
            return;
          }
          updateSubchannelPicker(newPicker);
          // It's not appropriate to report SHUTDOWN state from lb.
          // Ignore the case of newState == SHUTDOWN for now.
          if (newState != SHUTDOWN) {
            channelLogger.log(ChannelLogLevel.INFO, "Entering {0} state", newState);
            channelStateManager.gotoState(newState);
          }
        }
      }

      syncContext.execute(new UpdateBalancingState());
    }

    @Override
    public void updateSubchannelAddresses(
        LoadBalancer.Subchannel subchannel, List<EquivalentAddressGroup> addrs) {
      checkArgument(subchannel instanceof SubchannelImpl,
          "subchannel must have been returned from createSubchannel");
      ((SubchannelImpl) subchannel).subchannel.updateAddresses(addrs);
    }

    @Override
    public ManagedChannel createOobChannel(EquivalentAddressGroup addressGroup, String authority) {
      // TODO(ejona): can we be even stricter? Like terminating?
      checkState(!terminated, "Channel is terminated");
      long oobChannelCreationTime = timeProvider.currentTimeNanos();
      InternalLogId oobLogId = InternalLogId.allocate("OobChannel");
      InternalLogId subchannelLogId = InternalLogId.allocate("Subchannel-OOB");
      ChannelTracer oobChannelTracer =
          new ChannelTracer(
              oobLogId, maxTraceEvents, oobChannelCreationTime,
              "OobChannel for " + addressGroup);
      final OobChannel oobChannel = new OobChannel(
          authority, balancerRpcExecutorPool, transportFactory.getScheduledExecutorService(),
          syncContext, callTracerFactory.create(), oobChannelTracer, channelz, timeProvider);
      channelTracer.reportEvent(new ChannelTrace.Event.Builder()
          .setDescription("Child OobChannel created")
          .setSeverity(ChannelTrace.Event.Severity.CT_INFO)
          .setTimestampNanos(oobChannelCreationTime)
          .setChannelRef(oobChannel)
          .build());
      ChannelTracer subchannelTracer =
          new ChannelTracer(subchannelLogId, maxTraceEvents, oobChannelCreationTime,
              "Subchannel for " + addressGroup);
      final class ManagedOobChannelCallback extends InternalSubchannel.Callback {
        @Override
        void onTerminated(InternalSubchannel is) {
          oobChannels.remove(oobChannel);
          channelz.removeSubchannel(is);
          oobChannel.handleSubchannelTerminated();
          maybeTerminateChannel();
        }

        @Override
        void onStateChange(InternalSubchannel is, ConnectivityStateInfo newState) {
          handleInternalSubchannelState(newState);
          oobChannel.handleSubchannelStateChange(newState);
        }
      }

      final InternalSubchannel internalSubchannel = new InternalSubchannel(
          Collections.singletonList(addressGroup),
          authority, userAgent, backoffPolicyProvider, transportFactory,
          transportFactory.getScheduledExecutorService(), stopwatchSupplier, syncContext,
          // All callback methods are run from syncContext
          new ManagedOobChannelCallback(),
          channelz,
          callTracerFactory.create(),
          subchannelTracer,
          subchannelLogId,
          timeProvider);
      oobChannelTracer.reportEvent(new ChannelTrace.Event.Builder()
          .setDescription("Child Subchannel created")
          .setSeverity(ChannelTrace.Event.Severity.CT_INFO)
          .setTimestampNanos(oobChannelCreationTime)
          .setSubchannelRef(internalSubchannel)
          .build());
      channelz.addSubchannel(oobChannel);
      channelz.addSubchannel(internalSubchannel);
      oobChannel.setSubchannel(internalSubchannel);
      final class AddOobChannel implements Runnable {
        @Override
        public void run() {
          if (terminating) {
            oobChannel.shutdown();
          }
          if (!terminated) {
            // If channel has not terminated, it will track the subchannel and block termination
            // for it.
            oobChannels.add(oobChannel);
          }
        }
      }

      syncContext.execute(new AddOobChannel());
      return oobChannel;
    }

    @Override
    public void updateOobChannelAddresses(ManagedChannel channel, EquivalentAddressGroup eag) {
      checkArgument(channel instanceof OobChannel,
          "channel must have been returned from createOobChannel");
      ((OobChannel) channel).updateAddresses(eag);
    }

    @Override
    public String getAuthority() {
      return ManagedChannelImpl.this.authority();
    }

    @Override
    public NameResolver.Factory getNameResolverFactory() {
      return nameResolverFactory;
    }

    @Override
    public SynchronizationContext getSynchronizationContext() {
      return syncContext;
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
      return scheduledExecutorForBalancer;
    }

    @Override
    public ChannelLogger getChannelLogger() {
      return channelLogger;
    }
  }

  private class NameResolverListenerImpl implements NameResolver.Listener {
    final LbHelperImpl helper;
    Object argument;

    NameResolverListenerImpl(LbHelperImpl helperImpl, Object argument) {
      this.helper = helperImpl;
      this.argument = argument;
    }

    @Override
    public void onAddresses(final List<EquivalentAddressGroup> servers, final Attributes config) {
      if (servers.isEmpty()) {
        onError(Status.UNAVAILABLE.withDescription(
            "Name resolver " + helper.nr + " returned an empty list"));
        return;
      }
      channelLogger.log(
          ChannelLogLevel.DEBUG, "Resolved address: {0}, config={1}", servers, config);

      if (haveBackends == null || !haveBackends) {
        channelLogger.log(ChannelLogLevel.INFO, "Address resolved: {0}", servers);
        haveBackends = true;
      }
      final Map<String, Object> serviceConfig =
          config.get(GrpcAttributes.NAME_RESOLVER_SERVICE_CONFIG);
      if (serviceConfig != null && !serviceConfig.equals(lastServiceConfig)) {
        channelLogger.log(ChannelLogLevel.INFO, "Service config changed");
        lastServiceConfig = serviceConfig;
      }

      final class NamesResolved implements Runnable {
        @Override
        public void run() {
          // Call LB only if it's not shutdown.  If LB is shutdown, lbHelper won't match.
          if (NameResolverListenerImpl.this.helper != ManagedChannelImpl.this.lbHelper) {
            return;
          }

          nameResolverBackoffPolicy = null;

          if (serviceConfig != null) {
            try {
              serviceConfigInterceptor.handleUpdate(serviceConfig);
              if (retryEnabled) {
                throttle = getThrottle(config);
              }
            } catch (RuntimeException re) {
              logger.warn(
                  "[" + getLogId() + "] Unexpected exception from parsing service config",
                  re);
            }
          }

          helper.lb.handleResolvedAddressGroups(servers, config);
        }
      }

      syncContext.execute(new NamesResolved());
    }

    @Override
    public void onError(final Status error) {
      checkArgument(!error.isOk(), "the error status must not be OK");
      logger.warn("[{}] Failed to resolve name. status={}",
          new Object[] {getLogId(), error});
      if (haveBackends == null || haveBackends) {
        channelLogger.log(ChannelLogLevel.WARNING, "Failed to resolve name: {0}", error);
        haveBackends = false;
      }
      final class NameResolverErrorHandler implements Runnable {
        @Override
        public void run() {
          // Call LB only if it's not shutdown.  If LB is shutdown, lbHelper won't match.
          if (NameResolverListenerImpl.this.helper != ManagedChannelImpl.this.lbHelper) {
            return;
          }
          helper.lb.handleNameResolutionError(error);
          if (nameResolverRefreshFuture != null) {
            // The name resolver may invoke onError multiple times, but we only want to
            // schedule one backoff attempt
            // TODO(ericgribkoff) Update contract of NameResolver.Listener or decide if we
            // want to reset the backoff interval upon repeated onError() calls
            return;
          }
          if (nameResolverBackoffPolicy == null) {
            nameResolverBackoffPolicy = backoffPolicyProvider.get();
          }
          long delayNanos = nameResolverBackoffPolicy.nextBackoffNanos();
          channelLogger.log(
                ChannelLogLevel.DEBUG,
                "Scheduling DNS resolution backoff for {0} ns", delayNanos);
          nameResolverRefresh = new NameResolverRefresh();
          nameResolverRefreshFuture =
              transportFactory
                  .getScheduledExecutorService()
                  .schedule(nameResolverRefresh, delayNanos, TimeUnit.NANOSECONDS);
        }
      }

      syncContext.execute(new NameResolverErrorHandler());
    }

    @Override
    public Object getArgument() {
      return argument;
    }

    @Override
    public void setArgument(Object argument){
      this.argument = argument;
    }

    /**
     * 删除客户端与离线服务端之间的无效subchannel
     *
     * @author sxp
     * @since 2019/12/02
     */
    @Override
    public void removeInvalidCacheSubchannels(final Set<String> removeHostPorts) {
      if (removeHostPorts == null || removeHostPorts.isEmpty()) {
        return;
      }

      final class InvalidCacheHandler implements Runnable {
        @Override
        public void run() {
          if (NameResolverListenerImpl.this.helper != ManagedChannelImpl.this.lbHelper) {
            return;
          }
          helper.lb.removeInvalidCacheSubchannels(removeHostPorts);
        }
      }

      syncContext.execute(new InvalidCacheHandler());
    }
  }

  @Nullable
  private static Throttle getThrottle(Attributes config) {
    return ServiceConfigUtil.getThrottlePolicy(
        config.get(GrpcAttributes.NAME_RESOLVER_SERVICE_CONFIG));
  }

  private final class SubchannelImpl extends AbstractSubchannel {
    // Set right after SubchannelImpl is created.
    InternalSubchannel subchannel;
    final Object shutdownLock = new Object();
    final Attributes attrs;

    @GuardedBy("shutdownLock")
    boolean shutdownRequested;
    @GuardedBy("shutdownLock")
    ScheduledFuture<?> delayedShutdownTask;

    SubchannelImpl(Attributes attrs) {
      this.attrs = checkNotNull(attrs, "attrs");
    }

    @Override
    ClientTransport obtainActiveTransport() {
      return subchannel.obtainActiveTransport();
    }

    @Override
    InternalInstrumented<ChannelStats> getInternalSubchannel() {
      return subchannel;
    }

    @Override
    public void shutdown() {
      synchronized (shutdownLock) {
        if (shutdownRequested) {
          if (terminating && delayedShutdownTask != null) {
            // shutdown() was previously called when terminating == false, thus a delayed shutdown()
            // was scheduled.  Now since terminating == true, We should expedite the shutdown.
            delayedShutdownTask.cancel(false);
            delayedShutdownTask = null;
            // Will fall through to the subchannel.shutdown() at the end.
          } else {
            return;
          }
        } else {
          shutdownRequested = true;
        }
        // Add a delay to shutdown to deal with the race between 1) a transport being picked and
        // newStream() being called on it, and 2) its Subchannel is shut down by LoadBalancer (e.g.,
        // because of address change, or because LoadBalancer is shutdown by Channel entering idle
        // mode). If (2) wins, the app will see a spurious error. We work around this by delaying
        // shutdown of Subchannel for a few seconds here.
        //
        // TODO(zhangkun83): consider a better approach
        // (https://github.com/grpc/grpc-java/issues/2562).
        if (!terminating) {
          final class ShutdownSubchannel implements Runnable {
            @Override
            public void run() {
              subchannel.shutdown(SUBCHANNEL_SHUTDOWN_STATUS);
            }
          }

          delayedShutdownTask = transportFactory.getScheduledExecutorService().schedule(
              new LogExceptionRunnable(
                  new ShutdownSubchannel()), SUBCHANNEL_SHUTDOWN_DELAY_SECONDS, TimeUnit.SECONDS);
          return;
        }
      }
      // When terminating == true, no more real streams will be created. It's safe and also
      // desirable to shutdown timely.
      subchannel.shutdown(SHUTDOWN_STATUS);
    }

    @Override
    public void requestConnection() {
      subchannel.obtainActiveTransport();
    }

    @Override
    public List<EquivalentAddressGroup> getAllAddresses() {
      return subchannel.getAddressGroups();
    }

    @Override
    public Attributes getAttributes() {
      return attrs;
    }

    @Override
    public String toString() {
      return subchannel.getLogId().toString();
    }

    @Override
    public Channel asChannel() {
      return new SubchannelChannel(
          subchannel, balancerRpcExecutorHolder.getExecutor(),
          transportFactory.getScheduledExecutorService(),
          callTracerFactory.create());
    }

    @Override
    public ChannelLogger getChannelLogger() {
      return subchannel.getChannelLogger();
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("logId", logId.getId())
        .add("target", target)
        .toString();
  }

  /**
   * Called from syncContext.
   */
  private final class DelayedTransportListener implements ManagedClientTransport.Listener {
    @Override
    public void transportShutdown(Status s) {
      checkState(shutdown.get(), "Channel must have been shut down");
    }

    @Override
    public void transportReady() {
      // Don't care
    }

    @Override
    public void transportInUse(final boolean inUse) {
      inUseStateAggregator.updateObjectInUse(delayedTransport, inUse);
    }

    @Override
    public void transportTerminated() {
      checkState(shutdown.get(), "Channel must have been shut down");
      terminating = true;
      shutdownNameResolverAndLoadBalancer(false);
      // No need to call channelStateManager since we are already in SHUTDOWN state.
      // Until LoadBalancer is shutdown, it may still create new subchannels.  We catch them
      // here.
      maybeShutdownNowSubchannels();
      maybeTerminateChannel();
    }
  }

  /**
   * Must be accessed from syncContext.
   */
  private final class IdleModeStateAggregator extends InUseStateAggregator<Object> {
    @Override
    protected void handleInUse() {
      //----begin----获取一致性Hash的参数值----
      final Object argument = getArgument();
      //----end------获取一致性Hash的参数值----

      exitIdleMode(argument);
    }

    @Override
    protected void handleNotInUse() {
      if (shutdown.get()) {
        return;
      }
      rescheduleIdleTimer();
    }
  }

  /**
   * 获取一致性Hash负载均衡算法的参数值
   *
   * @author sxp
   * @since 2019/4/1
   */
  private Object getArgument() {
    String serviceName = null;
    if (nameResolver != null) {
      serviceName = nameResolver.getServiceName();
    }
    final Object argument = ConsistentHashArguments.getArgument(serviceName);
    if (argument != null) {
      ConsistentHashArguments.resetArgument(serviceName);
    }
    return argument;
  }

  /**
   * 获取负载均衡模式
   *
   * @Author yuanzhonglin
   * @since 2019/4/15
   */
  private String getloadBalanceMode(NameResolver nameResolver) {
    String method = getMethod(nameResolver);
    String mode = LoadBalanceUtil.getLoadBalanceMode(nameResolver, method);
    return mode;
  }

  /**
   * 当前调用的服务方法
   *
   * @Author yuanzhonglin
   * @since 2019/4/17
   */
  private String getMethod(NameResolver nameResolver) {
    String serviceName = nameResolver.getServiceName();
    String method = ThreadLocalVariableUtils.getServiceMethodName(serviceName);
    return method;
  }


  /**
   * Lazily request for Executor from an executor pool.
   */
  private static final class ExecutorHolder {
    private final ObjectPool<? extends Executor> pool;
    private Executor executor;

    ExecutorHolder(ObjectPool<? extends Executor> executorPool) {
      this.pool = checkNotNull(executorPool, "executorPool");
    }

    synchronized Executor getExecutor() {
      if (executor == null) {
        executor = checkNotNull(pool.getObject(), "%s.getObject()", executor);
      }
      return executor;
    }

    synchronized void release() {
      if (executor != null) {
        executor = pool.returnObject(executor);
      }
    }
  }

  private static final class ScheduledExecutorForBalancer implements ScheduledExecutorService {
    final ScheduledExecutorService delegate;

    private ScheduledExecutorForBalancer(ScheduledExecutorService delegate) {
      this.delegate = checkNotNull(delegate, "delegate");
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
      return delegate.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable cmd, long delay, TimeUnit unit) {
      return delegate.schedule(cmd, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(
        Runnable command, long initialDelay, long period, TimeUnit unit) {
      return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
        Runnable command, long initialDelay, long delay, TimeUnit unit) {
      return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
      return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException {
      return delegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(
        Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException {
      return delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException {
      return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
      return delegate.invokeAny(tasks, timeout, unit);
    }

    @Override
    public boolean isShutdown() {
      return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
      return delegate.isTerminated();
    }

    @Override
    public void shutdown() {
      throw new UnsupportedOperationException("Restricted: shutdown() is not allowed");
    }

    @Override
    public List<Runnable> shutdownNow() {
      throw new UnsupportedOperationException("Restricted: shutdownNow() is not allowed");
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
      return delegate.submit(task);
    }

    @Override
    public Future<?> submit(Runnable task) {
      return delegate.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
      return delegate.submit(task, result);
    }

    @Override
    public void execute(Runnable command) {
      delegate.execute(command);
    }
  }

  /**
   * 获取负载均衡模式集合
   *
   * @Author yuanzhonglin
   * @since 2019/4/15
   */
  @Override
  public Map<String, String> getLoadBalanceModeMap() {
    return loadBalanceModeMap;
  }

  /**
   * 设置负载均衡模式集合内容
   *
   * @Author yuanzhonglin
   * @since 2019/4/16
   */
  @Override
  public void setLoadBalanceModeMap(Map<String, String> map) {
    this.loadBalanceModeMap = map;
  }

  @Override
  public NameResolver getNameResolver() {
    return nameResolver;
  }

  @Override
  public LoadBalancer getLoadBalancer() {
    return this.lbHelper.lb;
  }
}
