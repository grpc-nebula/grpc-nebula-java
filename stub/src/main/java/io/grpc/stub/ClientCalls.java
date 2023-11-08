/*
 * Copyright 2014 The gRPC Authors
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

package io.grpc.stub;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.enums.LoadBalanceMode;
import com.orientsec.grpc.common.resource.SystemConfig;
import com.orientsec.grpc.common.util.GrpcUtils;
import com.orientsec.grpc.common.util.LoadBalanceUtil;
import com.orientsec.grpc.common.util.PropertiesUtils;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.consumer.ConsistentHashArguments;
import com.orientsec.grpc.consumer.FailoverUtils;
import com.orientsec.grpc.consumer.ThreadLocalVariableUtils;
import com.orientsec.grpc.consumer.internal.ZookeeperNameResolver;
import com.orientsec.grpc.consumer.qos.ConsumerRequestsControllerUtils;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility functions for processing different call idioms. We have one-to-one correspondence
 * between utilities in this class and the potential signatures in a generated stub class so
 * that the runtime can vary behavior without requiring regeneration of the stub.
 */
public final class ClientCalls {
  private static final Logger logger = LoggerFactory.getLogger(ClientCalls.class);

  private static Properties properties = SystemConfig.getProperties();

  /**
   * 失败重试次数
   */
  private static int failureRetryNum = initFailureRetryNum();


  private static int initFailureRetryNum() {
    String key = GlobalConstants.Consumer.Key.CONSUME_RDEFAULT_RETRIES;
    int defaultValue = 0;

    int num = PropertiesUtils.getValidIntegerValue(properties, key, defaultValue);
    if (num < 0) {
      num = defaultValue;
    }

    logger.info(key + " = " + num);

    return num;
  }

  // Prevent instantiation
  private ClientCalls() {}

  /**
   * Executes a unary call with a response {@link StreamObserver}.  The {@code call} should not be
   * already started.  After calling this method, {@code call} should no longer be used.
   */
  public static <ReqT, RespT> void asyncUnaryCall(
      ClientCall<ReqT, RespT> call, ReqT req, StreamObserver<RespT> responseObserver) {
    asyncUnaryRequestCall(call, req, responseObserver, false);
  }

  /**
   * Executes a server-streaming call with a response {@link StreamObserver}.  The {@code call}
   * should not be already started.  After calling this method, {@code call} should no longer be
   * used.
   */
  public static <ReqT, RespT> void asyncServerStreamingCall(
      ClientCall<ReqT, RespT> call, ReqT req, StreamObserver<RespT> responseObserver) {
    asyncUnaryRequestCall(call, req, responseObserver, true);
  }

  /**
   * Executes a client-streaming call returning a {@link StreamObserver} for the request messages.
   * The {@code call} should not be already started.  After calling this method, {@code call}
   * should no longer be used.
   *
   * @return request stream observer.
   */
  public static <ReqT, RespT> StreamObserver<ReqT> asyncClientStreamingCall(
      ClientCall<ReqT, RespT> call,
      StreamObserver<RespT> responseObserver) {
    return asyncStreamingRequestCall(call, responseObserver, false);
  }

  /**
   * Executes a bidirectional-streaming call.  The {@code call} should not be already started.
   * After calling this method, {@code call} should no longer be used.
   *
   * @return request stream observer.
   */
  public static <ReqT, RespT> StreamObserver<ReqT> asyncBidiStreamingCall(
      ClientCall<ReqT, RespT> call, StreamObserver<RespT> responseObserver) {
    return asyncStreamingRequestCall(call, responseObserver, true);
  }

  /**
   * Executes a unary call and blocks on the response.  The {@code call} should not be already
   * started.  After calling this method, {@code call} should no longer be used.
   *
   * @return the single response message.
   */
  public static <ReqT, RespT> RespT blockingUnaryCall(ClientCall<ReqT, RespT> call, ReqT req) {
    try {
      return getUnchecked(futureUnaryCall(call, req));
    } catch (RuntimeException e) {
      throw cancelThrow(call, e);
    } catch (Error e) {
      throw cancelThrow(call, e);
    }
  }

  /**
   * Executes a unary call and blocks on the response.  The {@code call} should not be already
   * started.  After calling this method, {@code call} should no longer be used.
   *
   * @return the single response message.
   */
  public static <ReqT, RespT> RespT blockingUnaryCall(
      Channel channel, MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, ReqT req) {
    ThreadlessExecutor executor = new ThreadlessExecutor();
    ClientCall<ReqT, RespT> call = channel.newCall(method, callOptions.withExecutor(executor));

    try {
      ListenableFuture<RespT> responseFuture = futureUnaryCall(call, req);
      judgeResponseFuture(responseFuture, executor);
      RespT result = getUnchecked(responseFuture);
      //----begin----计算请求次数、请求出错次数----
      FailoverUtils.recordRequest(channel, true, call, null);
      //----end------计算请求次数、请求出错次数----
      return result;
    } catch (RuntimeException e) {
      FailoverUtils.recordRequest(channel, false, call, e);
      RespT respT = failureRetry(channel, method, callOptions, req, call, executor);
      if (respT != null) {
        return respT;
      }
      throw cancelThrow(call, e);
    } catch (Error e) {
      FailoverUtils.recordRequest(channel, false, call, null);
      RespT respT = failureRetry(channel, method, callOptions, req, call, executor);
      if (respT != null) {
        return respT;
      }
      throw cancelThrow(call, e);
    }
  }

  /**
   * 重选服务器
   *
   * @Author yuanzhonglin
   * @since 2019/4/8
   */
  private static void reelectServer(Channel channel, String fullMethod){
    NameResolver nameResolver = channel.getNameResolver();
    if (nameResolver instanceof ZookeeperNameResolver) {

      ZookeeperNameResolver zkResolver = (ZookeeperNameResolver) nameResolver;
      String method = GrpcUtils.getSimpleMethodName(fullMethod);
      if (LoadBalanceMode.connection.name().equals(
              LoadBalanceUtil.getLoadBalanceMode(nameResolver, method))) {

        zkResolver.resolveServerInfo(getArgument(nameResolver), method);
      }
    }
  }

  /**
   * 获取Argument值
   *
   * @Author yuanzhonglin
   * @since 2019/4/8
   */
  private static Object getArgument(NameResolver nameResolver) {
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
   * 校验ResponseFuture
   *
   * @Author yuanzhonglin
   * @since 2019/4/8
   */
  private static <RespT> void judgeResponseFuture(ListenableFuture<RespT> responseFuture
          , ThreadlessExecutor executor){
    while (!responseFuture.isDone()) {
      try {
        executor.waitAndDrain();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw Status.CANCELLED
                .withDescription("Call was interrupted")
                .withCause(e)
                .asRuntimeException();
      }
    }
  }

  /**
   * 失败重试
   *
   * @Author yuanzhonglin
   * @since 2019/4/8
   */
  private static <ReqT, RespT> RespT failureRetry (
          Channel channel,
          MethodDescriptor<ReqT, RespT> method,
          CallOptions callOptions,
          ReqT req,
          ClientCall<ReqT, RespT> call,
          ThreadlessExecutor executor) {

    int retryNum, methodRetryNum, serviceRetryNum;

    /**
     * 1. 从配置文件中获取指定Method的重试次数
     * 2. 如果Method没有配置，则取服务的配置次数
     * 3. 如果服务没有配置，则取默认次数
     */
    String interfaceName = GrpcUtils.getInterfaceNameNoneException(method.getFullMethodName());
    String methodName = GrpcUtils.getSimpleMethodName(method.getFullMethodName());
    String serviceRetryConfKey = GlobalConstants.Consumer.Key.CONSUME_RDEFAULT_RETRIES + "[" + interfaceName + "]";
    String methodRetryConfKey = GlobalConstants.Consumer.Key.CONSUME_RDEFAULT_RETRIES + "[" + interfaceName + "." + methodName + "]";

    methodRetryNum = PropertiesUtils.getValidIntegerValue(properties, methodRetryConfKey, 0);
    serviceRetryNum = PropertiesUtils.getValidIntegerValue(properties, serviceRetryConfKey, 0);

    if (methodRetryNum > 0) {
      retryNum = methodRetryNum;
    } else if (serviceRetryNum > 0) {
      retryNum = serviceRetryNum;
    } else {
      retryNum = failureRetryNum;
    }

    if (retryNum == 0) {
      return null;
    }

    ListenableFuture<RespT> responseFuture;
    RespT result;

    for (int i = 0; i < retryNum; i++) {
      try {
        logger.info("失败重试第" + (i + 1) + "次...");

        reelectServer(channel, method.getFullMethodName());
        call = channel.newCall(method, callOptions.withExecutor(executor));
        responseFuture = futureUnaryCall(call, req);
        judgeResponseFuture(responseFuture, executor);
        result = getUnchecked(responseFuture);

        FailoverUtils.recordRequest(channel, true, call, null);
        return result;
      } catch (Exception e) {
        FailoverUtils.recordRequest(channel, false, call, e);
      }
    }

    return null;
  }

  /**
   * Executes a server-streaming call returning a blocking {@link Iterator} over the
   * response stream.  The {@code call} should not be already started.  After calling this method,
   * {@code call} should no longer be used.
   *
   * @return an iterator over the response stream.
   */
  // TODO(louiscryan): Not clear if we want to use this idiom for 'simple' stubs.
  public static <ReqT, RespT> Iterator<RespT> blockingServerStreamingCall(
      ClientCall<ReqT, RespT> call, ReqT req) {
    BlockingResponseStream<RespT> result = new BlockingResponseStream<RespT>(call);
    asyncUnaryRequestCall(call, req, result.listener(), true);
    return result;
  }

  /**
   * Executes a server-streaming call returning a blocking {@link Iterator} over the
   * response stream.  The {@code call} should not be already started.  After calling this method,
   * {@code call} should no longer be used.
   *
   * @return an iterator over the response stream.
   */
  // TODO(louiscryan): Not clear if we want to use this idiom for 'simple' stubs.
  public static <ReqT, RespT> Iterator<RespT> blockingServerStreamingCall(
      Channel channel, MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, ReqT req) {
    ThreadlessExecutor executor = new ThreadlessExecutor();
    ClientCall<ReqT, RespT> call = channel.newCall(method, callOptions.withExecutor(executor));
    BlockingResponseStream<RespT> result = new BlockingResponseStream<RespT>(call, executor);

    boolean success = true;
    Exception e = null;

    try {
      asyncUnaryRequestCall(call, req, result.listener(), true);
    } catch (Throwable t) {
      if (t instanceof Exception) {
        e = (Exception) t;
      }
      success = false;
    } finally {
      //----begin----计算请求次数、请求出错次数----
      FailoverUtils.recordRequest(channel, success, call, e);
      //----end------计算请求次数、请求出错次数----
    }

    return result;
  }

  /**
   * Executes a unary call and returns a {@link ListenableFuture} to the response.  The
   * {@code call} should not be already started.  After calling this method, {@code call} should no
   * longer be used.
   *
   * @return a future for the single response message.
   */
  public static <ReqT, RespT> ListenableFuture<RespT> futureUnaryCall(
      ClientCall<ReqT, RespT> call, ReqT req) {
    GrpcFuture<RespT> responseFuture = new GrpcFuture<RespT>(call);
    asyncUnaryRequestCall(call, req, new UnaryStreamToFuture<RespT>(responseFuture), false);
    return responseFuture;
  }

  /**
   * Returns the result of calling {@link Future#get()} interruptibly on a task known not to throw a
   * checked exception.
   *
   * <p>If interrupted, the interrupt is restored before throwing an exception..
   *
   * @throws java.util.concurrent.CancellationException
   *     if {@code get} throws a {@code CancellationException}.
   * @throws StatusRuntimeException if {@code get} throws an {@link ExecutionException}
   *     or an {@link InterruptedException}.
   */
  private static <V> V getUnchecked(Future<V> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw Status.CANCELLED
          .withDescription("Call was interrupted")
          .withCause(e)
          .asRuntimeException();
    } catch (ExecutionException e) {
      throw toStatusRuntimeException(e.getCause());
    }
  }

  /**
   * Wraps the given {@link Throwable} in a {@link StatusRuntimeException}. If it contains an
   * embedded {@link StatusException} or {@link StatusRuntimeException}, the returned exception will
   * contain the embedded trailers and status, with the given exception as the cause. Otherwise, an
   * exception will be generated from an {@link Status#UNKNOWN} status.
   */
  private static StatusRuntimeException toStatusRuntimeException(Throwable t) {
    Throwable cause = checkNotNull(t, "t");
    while (cause != null) {
      // If we have an embedded status, use it and replace the cause
      if (cause instanceof StatusException) {
        StatusException se = (StatusException) cause;
        return new StatusRuntimeException(se.getStatus(), se.getTrailers());
      } else if (cause instanceof StatusRuntimeException) {
        StatusRuntimeException se = (StatusRuntimeException) cause;
        return new StatusRuntimeException(se.getStatus(), se.getTrailers());
      }
      cause = cause.getCause();
    }
    return Status.UNKNOWN.withDescription("unexpected exception").withCause(t)
        .asRuntimeException();
  }

  /**
   * Cancels a call, and throws the exception.
   *
   * @param t must be a RuntimeException or Error
   */
  private static RuntimeException cancelThrow(ClientCall<?, ?> call, Throwable t) {
    try {
      call.cancel(null, t);
    } catch (Throwable e) {
      assert e instanceof RuntimeException || e instanceof Error;
      logger.error("RuntimeException encountered while closing call", e);
    }
    if (t instanceof RuntimeException) {
      throw (RuntimeException) t;
    } else if (t instanceof Error) {
      throw (Error) t;
    }
    // should be impossible
    throw new AssertionError(t);
  }

  private static <ReqT, RespT> void asyncUnaryRequestCall(
      ClientCall<ReqT, RespT> call, ReqT req, StreamObserver<RespT> responseObserver,
      boolean streamingResponse) {
    asyncUnaryRequestCall(
        call,
        req,
        new StreamObserverToCallListenerAdapter<ReqT, RespT>(
            responseObserver,
            new CallToStreamObserverAdapter<ReqT>(call),
            streamingResponse),
        streamingResponse);
  }

  private static <ReqT, RespT> void asyncUnaryRequestCall(
      ClientCall<ReqT, RespT> call,
      ReqT req,
      ClientCall.Listener<RespT> responseListener,
      boolean streamingResponse) {
    //----begin----获取一致性Hash的参数值，放入ThreadLocal变量中----
    Object value = getArgumentFromRequest(req);
    String serviceName = GrpcUtils.getInterfaceNameNoneException(call.getFullMethod());
    ConsistentHashArguments.setArgument(serviceName, value);
    //----end------获取一致性Hash的参数值，放入ThreadLocal变量中----

    //----begin----获取调用方法参数值，放入ThreadLocal变量中----
    ThreadLocalVariableUtils.setServiceMethodName(serviceName, call.getFullMethod());
    //----end------获取调用方法参数值，放入ThreadLocal变量中----

    startCall(call, responseListener, streamingResponse);
    try {
      call.sendMessage(req);
      call.halfClose();
    } catch (RuntimeException e) {
      throw cancelThrow(call, e);
    } catch (Error e) {
      throw cancelThrow(call, e);
    }
  }

  /**
   * 根据请求参数获取对应参数列表的值
   * <p>
   * 目前的方法是将参数列表中的各参数值转化为String拼接起来。 <br>
   * 对于参数列表也有一定的限制，不支持参数在嵌套的层次中，即必须在第一层。 <br>
   * 如果客户端为未配置参数列表，或者参数值列表不正确，则取按照参数名升序获取第一个非嵌套类型参数的参数值返回。  <br>
   * </p>
   *
   * @author sxp
   * @since 2019/2/1
   */
  public static Object getArgumentFromRequest(Object request) {
    Map<String, Boolean> validArgs = ConsistentHashArguments.getValidArgs();

    boolean isGetFirst = false;
    if (validArgs.size() == 0) {
      isGetFirst = true;
    }

    // 遍历入参的所有参数，取出参数列表对应的参数值
    GeneratedMessageV3 param;
    try {
      param = GeneratedMessageV3.class.cast(request);
    } catch (ClassCastException e) {
      return ConsistentHashArguments.NULL_VALUE;// 入参不是GeneratedMessageV3的子类
    }

    Map<Descriptors.FieldDescriptor, Object> fieldMap = param.getAllFields();
    Descriptors.FieldDescriptor field;
    String name;
    String value;
    Descriptors.FieldDescriptor.JavaType type;
    StringBuilder sb = new StringBuilder();

    for (Iterator<Descriptors.FieldDescriptor> keys = fieldMap.keySet().iterator(); keys.hasNext(); ) {
      field = keys.next();
      name = field.getName();

      if (!validArgs.containsKey(name) && !isGetFirst) {
        continue;
      }

      type = field.getJavaType();
      if (type.equals(Descriptors.FieldDescriptor.JavaType.MESSAGE)) {
        // 嵌套数据类型
      } else if (type.equals(Descriptors.FieldDescriptor.JavaType.ENUM)) {
        Descriptors.EnumValueDescriptor en = (Descriptors.EnumValueDescriptor) fieldMap.get(field);
        value = String.valueOf(en.toProto().getNumber());
        sb.append(value);
      } else {
        value = String.valueOf(fieldMap.get(field));
        sb.append(value);
      }

      if (isGetFirst && sb.length() > 0) {
        break;
      }
    }

    String result = sb.toString();

    if (StringUtils.isEmpty(result)) {
      return String.valueOf(System.currentTimeMillis());
    } else {
      return result;
    }
  }

  private static <ReqT, RespT> StreamObserver<ReqT> asyncStreamingRequestCall(
      ClientCall<ReqT, RespT> call,
      StreamObserver<RespT> responseObserver,
      boolean streamingResponse) {
    CallToStreamObserverAdapter<ReqT> adapter = new CallToStreamObserverAdapter<ReqT>(call);
    startCall(
        call,
        new StreamObserverToCallListenerAdapter<ReqT, RespT>(
            responseObserver, adapter, streamingResponse),
        streamingResponse);
    return adapter;
  }

  private static <ReqT, RespT> void startCall(
      ClientCall<ReqT, RespT> call,
      ClientCall.Listener<RespT> responseListener,
      boolean streamingResponse) {
    //----begin----客户端的流量控制----

    String fullMethodName = call.getFullMethod();

    try {
      if (ConsumerRequestsControllerUtils.isNeedRequestsControl(fullMethodName)) {
        ConsumerRequestsControllerUtils.addRequestNum(fullMethodName);
      }
    } catch (Throwable t) {
      throw cancelThrow(call, t);
    }

    //----end----客户端的流量控制----

    call.start(responseListener, new Metadata());
    if (streamingResponse) {
      call.request(1);
    } else {
      // Initially ask for two responses from flow-control so that if a misbehaving server sends
      // more than one responses, we can catch it and fail it in the listener.
      call.request(2);
    }
  }

  private static final class CallToStreamObserverAdapter<T> extends ClientCallStreamObserver<T> {
    private boolean frozen;
    private final ClientCall<T, ?> call;
    private Runnable onReadyHandler;
    private boolean autoFlowControlEnabled = true;

    // Non private to avoid synthetic class
    CallToStreamObserverAdapter(ClientCall<T, ?> call) {
      this.call = call;
    }

    private void freeze() {
      this.frozen = true;
    }

    @Override
    public void onNext(T value) {
      call.sendMessage(value);
    }

    @Override
    public void onError(Throwable t) {
      call.cancel("Cancelled by client with StreamObserver.onError()", t);
    }

    @Override
    public void onCompleted() {
      call.halfClose();
    }

    @Override
    public boolean isReady() {
      return call.isReady();
    }

    @Override
    public void setOnReadyHandler(Runnable onReadyHandler) {
      if (frozen) {
        throw new IllegalStateException("Cannot alter onReadyHandler after call started");
      }
      this.onReadyHandler = onReadyHandler;
    }

    @Override
    public void disableAutoInboundFlowControl() {
      if (frozen) {
        throw new IllegalStateException("Cannot disable auto flow control call started");
      }
      autoFlowControlEnabled = false;
    }

    @Override
    public void request(int count) {
      call.request(count);
    }

    @Override
    public void setMessageCompression(boolean enable) {
      call.setMessageCompression(enable);
    }

    @Override
    public void cancel(@Nullable String message, @Nullable Throwable cause) {
      call.cancel(message, cause);
    }
  }

  private static final class StreamObserverToCallListenerAdapter<ReqT, RespT>
      extends ClientCall.Listener<RespT> {
    private final StreamObserver<RespT> observer;
    private final CallToStreamObserverAdapter<ReqT> adapter;
    private final boolean streamingResponse;
    private boolean firstResponseReceived;

    // Non private to avoid synthetic class
    StreamObserverToCallListenerAdapter(
        StreamObserver<RespT> observer,
        CallToStreamObserverAdapter<ReqT> adapter,
        boolean streamingResponse) {
      this.observer = observer;
      this.streamingResponse = streamingResponse;
      this.adapter = adapter;
      if (observer instanceof ClientResponseObserver) {
        @SuppressWarnings("unchecked")
        ClientResponseObserver<ReqT, RespT> clientResponseObserver =
            (ClientResponseObserver<ReqT, RespT>) observer;
        clientResponseObserver.beforeStart(adapter);
      }
      adapter.freeze();
    }

    @Override
    public void onHeaders(Metadata headers) {
    }

    @Override
    public void onMessage(RespT message) {
      if (firstResponseReceived && !streamingResponse) {
        throw Status.INTERNAL
            .withDescription("More than one responses received for unary or client-streaming call")
            .asRuntimeException();
      }
      firstResponseReceived = true;
      observer.onNext(message);

      if (streamingResponse && adapter.autoFlowControlEnabled) {
        // Request delivery of the next inbound message.
        adapter.request(1);
      }
    }

    @Override
    public void onClose(Status status, Metadata trailers) {
      if (status.isOk()) {
        observer.onCompleted();
      } else {
        observer.onError(status.asRuntimeException(trailers));
      }
    }

    @Override
    public void onReady() {
      if (adapter.onReadyHandler != null) {
        adapter.onReadyHandler.run();
      }
    }
  }

  /**
   * Completes a {@link GrpcFuture} using {@link StreamObserver} events.
   */
  private static final class UnaryStreamToFuture<RespT> extends ClientCall.Listener<RespT> {
    private final GrpcFuture<RespT> responseFuture;
    private RespT value;

    // Non private to avoid synthetic class
    UnaryStreamToFuture(GrpcFuture<RespT> responseFuture) {
      this.responseFuture = responseFuture;
    }

    @Override
    public void onHeaders(Metadata headers) {
    }

    @Override
    public void onMessage(RespT value) {
      if (this.value != null) {
        throw Status.INTERNAL.withDescription("More than one value received for unary call")
            .asRuntimeException();
      }
      this.value = value;
    }

    @Override
    public void onClose(Status status, Metadata trailers) {
      if (status.isOk()) {
        if (value == null) {
          // No value received so mark the future as an error
          responseFuture.setException(
              Status.INTERNAL.withDescription("No value received for unary call")
                  .asRuntimeException(trailers));
        }
        responseFuture.set(value);
      } else {
        responseFuture.setException(status.asRuntimeException(trailers));
      }
    }
  }

  private static final class GrpcFuture<RespT> extends AbstractFuture<RespT> {
    private final ClientCall<?, RespT> call;

    // Non private to avoid synthetic class
    GrpcFuture(ClientCall<?, RespT> call) {
      this.call = call;
    }

    @Override
    protected void interruptTask() {
      call.cancel("GrpcFuture was cancelled", null);
    }

    @Override
    protected boolean set(@Nullable RespT resp) {
      return super.set(resp);
    }

    @Override
    protected boolean setException(Throwable throwable) {
      return super.setException(throwable);
    }

    @SuppressWarnings("MissingOverride") // Add @Override once Java 6 support is dropped
    protected String pendingToString() {
      return MoreObjects.toStringHelper(this).add("clientCall", call).toString();
    }
  }

  /**
   * Convert events on a {@link ClientCall.Listener} into a blocking {@link Iterator}.
   *
   * <p>The class is not thread-safe, but it does permit {@link ClientCall.Listener} calls in a
   * separate thread from {@link Iterator} calls.
   */
  // TODO(ejona86): determine how to allow ClientCall.cancel() in case of application error.
  private static final class BlockingResponseStream<T> implements Iterator<T> {
    // Due to flow control, only needs to hold up to 2 items: 1 for value, 1 for close.
    private final BlockingQueue<Object> buffer = new ArrayBlockingQueue<Object>(2);
    private final ClientCall.Listener<T> listener = new QueuingListener();
    private final ClientCall<?, T> call;
    /** May be null. */
    private final ThreadlessExecutor threadless;
    // Only accessed when iterating.
    private Object last;

    // Non private to avoid synthetic class
    BlockingResponseStream(ClientCall<?, T> call) {
      this(call, null);
    }

    // Non private to avoid synthetic class
    BlockingResponseStream(ClientCall<?, T> call, ThreadlessExecutor threadless) {
      this.call = call;
      this.threadless = threadless;
    }

    ClientCall.Listener<T> listener() {
      return listener;
    }

    private Object waitForNext() throws InterruptedException {
      if (threadless == null) {
        return buffer.take();
      } else {
        Object next = buffer.poll();
        while (next == null) {
          threadless.waitAndDrain();
          next = buffer.poll();
        }
        return next;
      }
    }

    @Override
    public boolean hasNext() {
      if (last == null) {
        try {
          // Will block here indefinitely waiting for content. RPC timeouts defend against permanent
          // hangs here as the call will become closed.
          last = waitForNext();
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw Status.CANCELLED.withDescription("interrupted").withCause(ie).asRuntimeException();
        }
      }
      if (last instanceof StatusRuntimeException) {
        // Rethrow the exception with a new stacktrace.
        StatusRuntimeException e = (StatusRuntimeException) last;
        throw e.getStatus().asRuntimeException(e.getTrailers());
      }
      return last != this;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      try {
        call.request(1);
        @SuppressWarnings("unchecked")
        T tmp = (T) last;
        return tmp;
      } finally {
        last = null;
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    private final class QueuingListener extends ClientCall.Listener<T> {
      // Non private to avoid synthetic class
      QueuingListener() {}

      private boolean done = false;

      @Override
      public void onHeaders(Metadata headers) {
      }

      @Override
      public void onMessage(T value) {
        Preconditions.checkState(!done, "ClientCall already closed");
        buffer.add(value);
      }

      @Override
      public void onClose(Status status, Metadata trailers) {
        Preconditions.checkState(!done, "ClientCall already closed");
        if (status.isOk()) {
          buffer.add(BlockingResponseStream.this);
        } else {
          buffer.add(status.asRuntimeException(trailers));
        }
        done = true;
      }
    }
  }

  private static final class ThreadlessExecutor implements Executor {
    private static final Logger log = LoggerFactory.getLogger(ThreadlessExecutor.class);

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

    // Non private to avoid synthetic class
    ThreadlessExecutor() {}

    /**
     * Waits until there is a Runnable, then executes it and all queued Runnables after it.
     */
    public void waitAndDrain() throws InterruptedException {
      Runnable runnable = queue.take();
      while (runnable != null) {
        try {
          runnable.run();
        } catch (Throwable t) {
          log.warn("Runnable threw exception", t);
        }
        runnable = queue.poll();
      }
    }

    @Override
    public void execute(Runnable runnable) {
      queue.add(runnable);
    }
  }
}
