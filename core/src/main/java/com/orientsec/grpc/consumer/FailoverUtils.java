/*
 * Copyright 2019 Orient Securities Co., Ltd.
 * Copyright 2019 BoCloud Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientsec.grpc.consumer;

import com.orientsec.grpc.common.collect.ConcurrentHashSet;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.resource.SystemConfig;
import com.orientsec.grpc.common.util.ExceptionUtils;
import com.orientsec.grpc.common.util.GrpcUtils;
import com.orientsec.grpc.common.util.IpPortUtils;
import com.orientsec.grpc.common.util.MathUtils;
import com.orientsec.grpc.common.util.Networks;
import com.orientsec.grpc.common.util.PropertiesUtils;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.orientsec.grpc.common.constant.GlobalConstants.ProviderStatus;


/**
 * 客户端容错工具类
 * <p>
 * (1)连续多次请求出错，自动切换到提供相同服务的新服务器（经确认，该逻辑还需要保留） <br>
 * (2)熔断机制
 * <p/>
 *
 * @author sxp
 * @since 2018/6/21
 */
public class FailoverUtils {
  private static final Logger logger = LoggerFactory.getLogger(FailoverUtils.class);

  static final String CONSUMERID_PROVIDERID_SEPARATOR = "@";

  private static Properties properties = SystemConfig.getProperties();


  // 是否启用熔断机制
  private static boolean enabled = initBreakerEnabled();

  // 熔断机制统计周期，单位毫秒
  private static int periodMillis = initBreakerPeriod();

  // 在一个统计周期中至少请求多少次才会触发熔断机制
  private static int requestThreshold = initBreakerRequestThreshold();

  // 熔断器打开的错误百分比阈值
  private static int errorPercentage = initBreakerErrorPercentage();

  // 熔断器打开后经过多长时间允许一次请求尝试执行，单位毫秒
  private static int breakerSleepWindowMillis = initBreakerSleepWindowInMilliseconds();

  /**
   * 总请求次数
   * <p>
   * key: consumerId@IP:port   <br>
   * <p/>
   */
  private volatile static ConcurrentHashMap<String, AtomicLong> totalRequestTimes = new ConcurrentHashMap<>();

  /**
   * 请求失败次数
   * <p>
   * key:consumerId@IP:port  <br>
   * <p/>
   */
  private volatile static ConcurrentHashMap<String, AtomicLong> failRequestTimes = new ConcurrentHashMap<>();

  /**
   * 一个统计熔断周期结束的时间毫秒数
   * <p>
   * key：consumerId@IP:port  <br>
   * value: 毫秒时间戳记录   <br>
   * <p/>
   */
  private volatile static ConcurrentHashMap<String, Long> endCountTimeMillis = new ConcurrentHashMap<>();

  /**
   * 客户端打开熔断器的时间
   * <p>
   * key: consumerId@IP:port      <br>
   * value: 毫秒时间戳
   * <p/>
   */
  private volatile static ConcurrentHashMap<String, Long> openBreakerTime = new ConcurrentHashMap<>();

  /**
   * 请求失败的服务端
   * <p>
   * key: consumerId@IP:port       <br>
   * value: 服务端的状态，详见com.orientsec.grpc.common.constant.GlobalConstants.ProviderStatus
   * <p/>
   */
  private volatile static ConcurrentHashMap<String, String> failProviders = new ConcurrentHashMap<>();

  /**
   * 调用失败的【客户端对应服务提供者列表】
   * <p>
   * key值为：consumerId  <br>
   * value值为: 服务提供者IP:port的列表  <br>
   * <p/>
   */
  private volatile static ConcurrentHashMap<String, ConcurrentHashSet<String>> consumerFailProviderIds = new ConcurrentHashMap<>();

  /**
   * 半熔断使用的线程池
   */
  private volatile static ScheduledExecutorService timerService = null;

  /**
   * 熔断转变为半熔断的处理线程
   *
   * @author yulei
   * @since 2019-09-02
   */
  private static class HalfBreakerRunnable implements Runnable {
    private NameResolver nameResolver;
    private String method;
    private String consumerId;
    private String providerId;

    public HalfBreakerRunnable(NameResolver nameResolver, String method,
                               String consumerId, String providerId) {
      this.nameResolver = nameResolver;
      this.method = method;
      this.consumerId = consumerId;
      this.providerId = providerId;
    }

    @Override
    public void run() {
      String key = consumerId + CONSUMERID_PROVIDERID_SEPARATOR + providerId;

      if (!failProviders.containsKey(key) || !openBreakerTime.containsKey(key)) {
        return;
      }

      Map<String, ServiceProvider> allProviders = nameResolver.getAllProviders();
      if (allProviders == null || allProviders.isEmpty()
              || !allProviders.containsKey(providerId)) {
        return;
      }

      ServiceProvider serviceProvider;
      Map<String, ServiceProvider> providersForLoadBalance;
      String status = failProviders.get(key);

      if (ProviderStatus.BREAKER.equals(status)) {
        providersForLoadBalance = nameResolver.getProvidersForLoadBalance();
        serviceProvider = allProviders.get(providerId);

        if (serviceProvider != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("将服务[" + providerId + "]标识为半熔断，再重新放到服务列表中");
          }
          failProviders.put(key, ProviderStatus.HALF_BREAKER);
          providersForLoadBalance.put(providerId, serviceProvider);
          nameResolver.reCalculateProvidersCountAfterLoadBalance(method);
        }
      }
    }
  }

  /**
   * 是否启用熔断机制
   */
  private static boolean initBreakerEnabled() {
    String key = GlobalConstants.CommonKey.BREAKER_ENABLED;
    boolean value = PropertiesUtils.getValidBooleanValue(properties, key, true);
    logger.info(key + " = " + value);
    return value;
  }

  /**
   * 初始化熔断机制统计周期，单位毫秒
   */
  private static int initBreakerPeriod() {
    String key = GlobalConstants.CommonKey.BREAKER_PERIOD;
    int defaultValue = 60000;
    int value = PropertiesUtils.getValidIntegerValue(properties, key, defaultValue);
    if (value <= 0) {
      value = defaultValue;
    }

    logger.info(key + " = " + value);
    return value;
  }

  /**
   * 初始化一个统计周期中至少请求多少次才会触发熔断机制
   */
  private static int initBreakerRequestThreshold() {
    String key = GlobalConstants.CommonKey.BREAKER_REQUEST_THRESHOLD;
    int defaultValue = 20;
    int value = PropertiesUtils.getValidIntegerValue(properties, key, defaultValue);
    if (value <= 0) {
      value = defaultValue;
    }

    logger.info(key + " = " + value);
    return value;
  }

  /**
   * 初始化熔断器打开的错误百分比阈值
   */
  private static int initBreakerErrorPercentage() {
    String key = GlobalConstants.CommonKey.BREAKER_ERROR_PERCENTAGE;
    int defaultValue = 50;
    int value = PropertiesUtils.getValidIntegerValue(properties, key, defaultValue);
    if (value <= 0) {
      value = defaultValue;
    }

    logger.info(key + " = " + value);
    return value;
  }

  /**
   * 初始化熔断器打开后经过多长时间允许一次请求尝试执行，单位毫秒
   */
  private static int initBreakerSleepWindowInMilliseconds() {
    String key = GlobalConstants.CommonKey.BREAKER_SLEEPWINDOWINMiLLISECONDS;
    int defaultValue = 60000;
    int value = PropertiesUtils.getValidIntegerValue(properties, key, defaultValue);
    if (value <= 0) {
      value = defaultValue;
    }

    logger.info(key + " = " + value);
    return value;
  }

  // --------------------------------------------------------

  /**
   * 统计在一个统计周期中总请求次数、出错次数，并计算是否进行熔断
   *
   * @author yulei
   * @since 2019-07-22
   * @since 2019-08-27 modify by sxp 代码完善
   */
  public static <ReqT, RespT> void recordRequest(Channel channel, boolean success, ClientCall<ReqT, RespT> call, Exception e) {
    if (channel == null) {
      return;
    }

    NameResolver nameResolver = channel.getNameResolver();
    if (nameResolver == null) {
      return;
    }

    String consumerId = nameResolver.getSubscribeId();
    if (StringUtils.isEmpty(consumerId)) {
      return;
    }

    if (success && e != null) {
      logger.error("服务调用成功，怎么还有异常信息呢???");
      e = null;
    }

    String providerId = getProviderId(channel, e);
    if (StringUtils.isEmpty(providerId)) {
      return;
    }

    String key = consumerId + CONSUMERID_PROVIDERID_SEPARATOR + providerId;

    String method = GrpcUtils.getSimpleMethodName(call.getFullMethod());

    long currentTime = System.currentTimeMillis();

    // 连续多次请求出错，自动切换到提供相同服务的新服务器
    ErrorNumberUtil.recordInvokeInfo(call, channel, method, success, e);

    // 熔断机制
    if (!enabled) {
      return;
    }

    if (timerService == null) {
      timerService = SharedResourceHolder.get(GrpcUtil.TIMER_SERVICE);
    }

    // 对于半熔断的服务端，如果该请求执行成功，说明服务可能已经恢复了正常，关闭熔断器；
    // 如果该请求执行失败，则认为服务依然不可用，熔断器继续保持打开状态
    if (failProviders.containsKey(key)) {
      String status = failProviders.get(key);
      if (ProviderStatus.HALF_BREAKER.equals(status)) {
        if (success) {
          failProviders.remove(key);
          openBreakerTime.remove(key);

          ConcurrentHashSet<String> ids = consumerFailProviderIds.get(consumerId);
          if (ids != null) {
            ids.remove(providerId);
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("半熔断的服务端[" + providerId + "]不可用，继续保持熔断状态");
          }
          failProviders.put(key, ProviderStatus.BREAKER);
          openBreakerTime.put(key, currentTime);
          removeCurrentProvider(nameResolver, providerId, method);
          saveFailProviderId(consumerId, providerId);
          // 半熔断的处理
          HalfBreakerRunnable runnable = new HalfBreakerRunnable(nameResolver, method, consumerId, providerId);
          timerService.schedule(runnable, breakerSleepWindowMillis, TimeUnit.MILLISECONDS);
        }
      }
    }

    if (!totalRequestTimes.containsKey(key)) {
      AtomicLong totalTimes = new AtomicLong(1);
      AtomicLong oldValue = totalRequestTimes.putIfAbsent(key, totalTimes);
      if (oldValue != null) {
        oldValue.incrementAndGet();
      }

      AtomicLong failTimes;
      if (!success) {
        failTimes = new AtomicLong(1);
      } else {
        failTimes = new AtomicLong(0);
      }

      oldValue = failRequestTimes.putIfAbsent(key, failTimes);
      if (oldValue != null) {
        if (!success) {
          oldValue.incrementAndGet();
        }
      }
    } else {
      AtomicLong totalTimes = totalRequestTimes.get(key);
      totalTimes.incrementAndGet();

      if (!success) {
        AtomicLong failTimes = failRequestTimes.get(key);
        failTimes.incrementAndGet();
      }
    }

    // 当前请求第一次发生，或者已经发生过的请求开启新的统计周期
    if (!endCountTimeMillis.containsKey(key) || endCountTimeMillis.get(key) == 0L) {
      long endTime = currentTime + periodMillis;
      endCountTimeMillis.put(key, endTime);
    } else {
      // 一个统计周期结束的处理
      long endTimeMillis = endCountTimeMillis.get(key);
      if (currentTime >= endTimeMillis) {
        AtomicLong totalCounter = totalRequestTimes.get(key);
        AtomicLong failCounter = failRequestTimes.get(key);

        long total = (totalCounter == null) ? 0 : totalCounter.get();
        long fail = (failCounter == null) ? 0 : failCounter.get();

        if (total >= requestThreshold) {
          int percent = (int) MathUtils.round(100D * fail / total, 0);

          if (percent >= errorPercentage) {
            logger.info("客户端调用服务端[" + providerId + "]时错误率为[" + percent + "]，开启熔断器");

            failProviders.put(key, ProviderStatus.BREAKER);
            openBreakerTime.put(key, currentTime);
            removeCurrentProvider(nameResolver, providerId, method);
            saveFailProviderId(consumerId, providerId);
            // 半熔断的处理
            HalfBreakerRunnable runnable = new HalfBreakerRunnable(nameResolver, method, consumerId, providerId);
            timerService.schedule(runnable, breakerSleepWindowMillis, TimeUnit.MILLISECONDS);
          }
        }

        // 一个统计周期完成后将数量、时间归零
        if (totalCounter != null) {
          totalCounter.set(0L);
        }
        if (failCounter != null) {
          failCounter.set(0L);
        }
        endCountTimeMillis.put(key, 0L);
      }
    }
  }


  /**
   * 客户端调用失败的服务端唯一标识
   *
   * @author sxp
   * @since 2019/8/26
   */
  private static void saveFailProviderId(String consumerId, String providerId) {
    ConcurrentHashSet<String> providerIds;

    if (!consumerFailProviderIds.containsKey(consumerId)) {
      providerIds = new ConcurrentHashSet<>();
      providerIds.add(providerId);

      ConcurrentHashSet<String> oldValue = consumerFailProviderIds.putIfAbsent(consumerId, providerIds);
      if (oldValue != null) {
        oldValue.add(providerId);
      }
    } else {
      providerIds = consumerFailProviderIds.get(consumerId);
      providerIds.add(providerId);
    }
  }

  /**
   * 将当前出错的服务器从备选列表中去除
   *
   * @author sxp
   * @since 2018-6-21
   */
  private static void removeCurrentProvider(NameResolver nameResolver, String providerId, String method) {
    Map<String, ServiceProvider> providersForLoadBalance = nameResolver.getProvidersForLoadBalance();
    if (providersForLoadBalance == null || providersForLoadBalance.size() == 0) {
      return;
    }

    if (providersForLoadBalance.containsKey(providerId)) {
      if (logger.isDebugEnabled()) {
        logger.debug("将当前出错的服务器{}从备选列表中删除", providerId);
      }
      providersForLoadBalance.remove(providerId);
      nameResolver.reCalculateProvidersCountAfterLoadBalance(method);

      int size = providersForLoadBalance.size();
      if (size > 0) {
        try {
          Object argument = getArgument(nameResolver);
          logger.info("重选服务提供者......");
          nameResolver.resolveServerInfo(argument, method);
        } catch (Throwable t) {
          logger.warn("重选服务提供者出错", t);
        }
      }
    }
  }

  /**
   * 获取一致性Hash负载均衡算法的参数值
   *
   * @author sxp
   * @since 2019/8/27
   */
  static Object getArgument(NameResolver nameResolver) {
    String serviceName = null;
    if (nameResolver != null) {
      serviceName = nameResolver.getServiceName();
    }
    Object argument = ConsistentHashArguments.getArgument(serviceName);
    return argument;
  }

  /**
   * 根据nameResolver获取对应的服务提供者Id
   *
   * @return 返回服务提供者Id，以IP:port的形式表示
   * @author sxp
   * @since 2018-6-25
   * @since 2019-11-19 modify by sxp 将获取host:port的代码独立为方法
   */
  static String getProviderId(Channel channel, Exception e) {
    // 先尝试从Exception中获取出错的服务端地址
    /*
    io.grpc.StatusRuntimeException: UNAVAILABLE: io exception
      at io.grpc.stub.ClientCalls.toStatusRuntimeException(ClientCalls.java:414)
      ...
    Caused by: io.netty.channel.AbstractChannel$AnnotatedConnectException: Connection refused: no further information: /192.168.106.3:50062
      at sun.nio.ch.SocketChannelImpl.checkConnect(Native Method)
      ...
    */
    if (e != null && (e instanceof StatusRuntimeException)) {
      StatusRuntimeException statusException = (StatusRuntimeException) e;
      Status status = statusException.getStatus();
      if (Status.Code.UNAVAILABLE.equals(status.getCode())) {
        String errorMsg = ExceptionUtils.getExceptionStackMsg(e);
        if (StringUtils.isNotEmpty(errorMsg)) {
          int startIndex = errorMsg.indexOf("Connection refused: no further information");
          if (startIndex >= 0) {
            int endIndex = errorMsg.indexOf("\n", startIndex);
            if (endIndex >= 0) {
              String message = errorMsg.substring(startIndex, endIndex);
              // 提取出IP:port
              String ipAndPort = IpPortUtils.getAddress(message);
              if (StringUtils.isNotEmpty(ipAndPort)) {
                return ipAndPort;
              }
            }
          }
        }
      } else if (Status.Code.CANCELLED.equals(status.getCode())) {
        // 被客户端主动取消的调用不计入错误提供者
        return null;
      }
    }

    if (channel == null) {
      return null;
    }

    LoadBalancer loadBalancer = channel.getLoadBalancer();
    if (loadBalancer == null) {
      return null;
    }

    EquivalentAddressGroup addressGroup = loadBalancer.getAddresses();
    if (addressGroup == null) {
      return null;
    }

    List<SocketAddress> addrs = addressGroup.getAddrs();
    if (addrs == null || addrs.size() == 0) {
      return null;
    }

    SocketAddress address = addrs.get(0);

    String providerId = Networks.getHostAndPort(address);

    return providerId;
  }

  /**
   * 删除与当前客户端相关的数据
   *
   * @author sxp
   * @since 2018-6-25
   */
  public static void removeDateByConsumerId(String consumerId) {
    // 【连续多次请求出错，自动切换到提供相同服务的新服务器】的变量
    ErrorNumberUtil.removeDateByConsumerId(consumerId);

    // 熔断机制的变量
    if (!consumerFailProviderIds.containsKey(consumerId)) {
      return;
    }

    ConcurrentHashSet<String> providerIds = consumerFailProviderIds.get(consumerId);

    String key;

    for (String providerId : providerIds) {
      key = consumerId + CONSUMERID_PROVIDERID_SEPARATOR + providerId;
      failProviders.remove(key);
      openBreakerTime.remove(key);
      totalRequestTimes.remove(key);
      failRequestTimes.remove(key);
      endCountTimeMillis.remove(key);
    }

    consumerFailProviderIds.remove(consumerId);
  }

}
