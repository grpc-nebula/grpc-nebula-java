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
package com.orientsec.grpc.service;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.1.2)",
    comments = "Source: greeter.proto")
public class GreeterGrpc {

  private GreeterGrpc() {}

  public static final String SERVICE_NAME = "com.orientsec.grpc.service.Greeter";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<GreeterRequest,
      GreeterReply> METHOD_SAY_HELLO =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "com.orientsec.grpc.service.Greeter", "sayHello"),
          io.grpc.protobuf.ProtoUtils.marshaller(GreeterRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(GreeterReply.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<CommonRequest,
      CommonReply> METHOD_DO_SOMETHING_FOR_LONG_TIME =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "com.orientsec.grpc.service.Greeter", "doSomethingForLongTime"),
          io.grpc.protobuf.ProtoUtils.marshaller(CommonRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(CommonReply.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<CommonRequest,
      CommonReply> METHOD_CHECK_METHOD_INVOKED =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "com.orientsec.grpc.service.Greeter", "checkMethodInvoked"),
          io.grpc.protobuf.ProtoUtils.marshaller(CommonRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(CommonReply.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static GreeterStub newStub(io.grpc.Channel channel) {
    return new GreeterStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static GreeterBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new GreeterBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static GreeterFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new GreeterFutureStub(channel);
  }

  /**
   */
  public static abstract class GreeterImplBase implements io.grpc.BindableService {

    /**
     */
    public void sayHello(GreeterRequest request,
        io.grpc.stub.StreamObserver<GreeterReply> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_SAY_HELLO, responseObserver);
    }

    /**
     */
    public void doSomethingForLongTime(CommonRequest request,
        io.grpc.stub.StreamObserver<CommonReply> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_DO_SOMETHING_FOR_LONG_TIME, responseObserver);
    }

    /**
     */
    public void checkMethodInvoked(CommonRequest request,
        io.grpc.stub.StreamObserver<CommonReply> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_CHECK_METHOD_INVOKED, responseObserver);
    }

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_SAY_HELLO,
            asyncUnaryCall(
              new MethodHandlers<
                GreeterRequest,
                GreeterReply>(
                  this, METHODID_SAY_HELLO)))
          .addMethod(
            METHOD_DO_SOMETHING_FOR_LONG_TIME,
            asyncUnaryCall(
              new MethodHandlers<
                CommonRequest,
                CommonReply>(
                  this, METHODID_DO_SOMETHING_FOR_LONG_TIME)))
          .addMethod(
            METHOD_CHECK_METHOD_INVOKED,
            asyncUnaryCall(
              new MethodHandlers<
                CommonRequest,
                CommonReply>(
                  this, METHODID_CHECK_METHOD_INVOKED)))
          .build();
    }
  }

  /**
   */
  public static final class GreeterStub extends io.grpc.stub.AbstractStub<GreeterStub> {
    private GreeterStub(io.grpc.Channel channel) {
      super(channel);
    }

    private GreeterStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected GreeterStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new GreeterStub(channel, callOptions);
    }

    /**
     */
    public void sayHello(GreeterRequest request,
        io.grpc.stub.StreamObserver<GreeterReply> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_SAY_HELLO, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void doSomethingForLongTime(CommonRequest request,
        io.grpc.stub.StreamObserver<CommonReply> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_DO_SOMETHING_FOR_LONG_TIME, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void checkMethodInvoked(CommonRequest request,
        io.grpc.stub.StreamObserver<CommonReply> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_CHECK_METHOD_INVOKED, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class GreeterBlockingStub extends io.grpc.stub.AbstractStub<GreeterBlockingStub> {
    private GreeterBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private GreeterBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected GreeterBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new GreeterBlockingStub(channel, callOptions);
    }

    /**
     */
    public GreeterReply sayHello(GreeterRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_SAY_HELLO, getCallOptions(), request);
    }

    /**
     */
    public CommonReply doSomethingForLongTime(CommonRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DO_SOMETHING_FOR_LONG_TIME, getCallOptions(), request);
    }

    /**
     */
    public CommonReply checkMethodInvoked(CommonRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_CHECK_METHOD_INVOKED, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class GreeterFutureStub extends io.grpc.stub.AbstractStub<GreeterFutureStub> {
    private GreeterFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private GreeterFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected GreeterFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new GreeterFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<GreeterReply> sayHello(
        GreeterRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_SAY_HELLO, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<CommonReply> doSomethingForLongTime(
        CommonRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_DO_SOMETHING_FOR_LONG_TIME, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<CommonReply> checkMethodInvoked(
        CommonRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_CHECK_METHOD_INVOKED, getCallOptions()), request);
    }
  }

  private static final int METHODID_SAY_HELLO = 0;
  private static final int METHODID_DO_SOMETHING_FOR_LONG_TIME = 1;
  private static final int METHODID_CHECK_METHOD_INVOKED = 2;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final GreeterImplBase serviceImpl;
    private final int methodId;

    public MethodHandlers(GreeterImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SAY_HELLO:
          serviceImpl.sayHello((GreeterRequest) request,
              (io.grpc.stub.StreamObserver<GreeterReply>) responseObserver);
          break;
        case METHODID_DO_SOMETHING_FOR_LONG_TIME:
          serviceImpl.doSomethingForLongTime((CommonRequest) request,
              (io.grpc.stub.StreamObserver<CommonReply>) responseObserver);
          break;
        case METHODID_CHECK_METHOD_INVOKED:
          serviceImpl.checkMethodInvoked((CommonRequest) request,
              (io.grpc.stub.StreamObserver<CommonReply>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static final class GreeterDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return GreeterProto.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (GreeterGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new GreeterDescriptorSupplier())
              .addMethod(METHOD_SAY_HELLO)
              .addMethod(METHOD_DO_SOMETHING_FOR_LONG_TIME)
              .addMethod(METHOD_CHECK_METHOD_INVOKED)
              .build();
        }
      }
    }
    return result;
  }
}
