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
public class PersonSalaryGrpc {

  private PersonSalaryGrpc() {}

  public static final String SERVICE_NAME = "com.orientsec.grpc.service.PersonSalary";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<PersonRequest,
      PersonReply> METHOD_QUERY_SALARY =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "com.orientsec.grpc.service.PersonSalary", "querySalary"),
          io.grpc.protobuf.ProtoUtils.marshaller(PersonRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(PersonReply.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PersonSalaryStub newStub(io.grpc.Channel channel) {
    return new PersonSalaryStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PersonSalaryBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new PersonSalaryBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static PersonSalaryFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new PersonSalaryFutureStub(channel);
  }

  /**
   */
  public static abstract class PersonSalaryImplBase implements io.grpc.BindableService {

    /**
     */
    public void querySalary(PersonRequest request,
        io.grpc.stub.StreamObserver<PersonReply> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_QUERY_SALARY, responseObserver);
    }

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_QUERY_SALARY,
            asyncUnaryCall(
              new MethodHandlers<
                PersonRequest,
                PersonReply>(
                  this, METHODID_QUERY_SALARY)))
          .build();
    }
  }

  /**
   */
  public static final class PersonSalaryStub extends io.grpc.stub.AbstractStub<PersonSalaryStub> {
    private PersonSalaryStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PersonSalaryStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected PersonSalaryStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PersonSalaryStub(channel, callOptions);
    }

    /**
     */
    public void querySalary(PersonRequest request,
        io.grpc.stub.StreamObserver<PersonReply> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_QUERY_SALARY, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class PersonSalaryBlockingStub extends io.grpc.stub.AbstractStub<PersonSalaryBlockingStub> {
    private PersonSalaryBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PersonSalaryBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected PersonSalaryBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PersonSalaryBlockingStub(channel, callOptions);
    }

    /**
     */
    public PersonReply querySalary(PersonRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_QUERY_SALARY, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class PersonSalaryFutureStub extends io.grpc.stub.AbstractStub<PersonSalaryFutureStub> {
    private PersonSalaryFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PersonSalaryFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected PersonSalaryFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PersonSalaryFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<PersonReply> querySalary(
        PersonRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_QUERY_SALARY, getCallOptions()), request);
    }
  }

  private static final int METHODID_QUERY_SALARY = 0;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PersonSalaryImplBase serviceImpl;
    private final int methodId;

    public MethodHandlers(PersonSalaryImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_QUERY_SALARY:
          serviceImpl.querySalary((PersonRequest) request,
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

  private static final class PersonSalaryDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return PersonProto.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (PersonSalaryGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PersonSalaryDescriptorSupplier())
              .addMethod(METHOD_QUERY_SALARY)
              .build();
        }
      }
    }
    return result;
  }
}
