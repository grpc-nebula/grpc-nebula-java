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

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.1.2)",
    comments = "Source: person.proto")
public class PersonInfoGrpc {

  private PersonInfoGrpc() {}

  public static final String SERVICE_NAME = "com.orientsec.grpc.service.PersonInfo";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<PersonRequest,
      PersonReply> METHOD_QUERY =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "com.orientsec.grpc.service.PersonInfo", "query"),
          io.grpc.protobuf.ProtoUtils.marshaller(PersonRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(PersonReply.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PersonInfoStub newStub(io.grpc.Channel channel) {
    return new PersonInfoStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PersonInfoBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new PersonInfoBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static PersonInfoFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new PersonInfoFutureStub(channel);
  }

  /**
   */
  public static abstract class PersonInfoImplBase implements io.grpc.BindableService {

    /**
     */
    public void query(PersonRequest request,
        io.grpc.stub.StreamObserver<PersonReply> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_QUERY, responseObserver);
    }

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_QUERY,
            asyncUnaryCall(
              new MethodHandlers<
                PersonRequest,
                PersonReply>(
                  this, METHODID_QUERY)))
          .build();
    }
  }

  /**
   */
  public static final class PersonInfoStub extends io.grpc.stub.AbstractStub<PersonInfoStub> {
    private PersonInfoStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PersonInfoStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected PersonInfoStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PersonInfoStub(channel, callOptions);
    }

    /**
     */
    public void query(PersonRequest request,
        io.grpc.stub.StreamObserver<PersonReply> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_QUERY, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class PersonInfoBlockingStub extends io.grpc.stub.AbstractStub<PersonInfoBlockingStub> {
    private PersonInfoBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PersonInfoBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected PersonInfoBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PersonInfoBlockingStub(channel, callOptions);
    }

    /**
     */
    public PersonReply query(PersonRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_QUERY, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class PersonInfoFutureStub extends io.grpc.stub.AbstractStub<PersonInfoFutureStub> {
    private PersonInfoFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PersonInfoFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected PersonInfoFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PersonInfoFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<PersonReply> query(
        PersonRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_QUERY, getCallOptions()), request);
    }
  }

  private static final int METHODID_QUERY = 0;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PersonInfoImplBase serviceImpl;
    private final int methodId;

    public MethodHandlers(PersonInfoImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_QUERY:
          serviceImpl.query((PersonRequest) request,
              (io.grpc.stub.StreamObserver<PersonReply>) responseObserver);
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

  private static final class PersonInfoDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return PersonProto.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (PersonInfoGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PersonInfoDescriptorSupplier())
              .addMethod(METHOD_QUERY)
              .build();
        }
      }
    }
    return result;
  }
}
