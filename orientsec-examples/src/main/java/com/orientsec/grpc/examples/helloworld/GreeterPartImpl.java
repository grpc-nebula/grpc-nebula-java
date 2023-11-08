package com.orientsec.grpc.examples.helloworld;

import io.grpc.stub.StreamObserver;

/**
 * Greeter服务部分实现类
 *
 * @author sxp
 * @since 2019/7/16
 */
public class GreeterPartImpl extends GreeterGrpc.GreeterImplBase {
  @Override
  public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
    System.out.println("server-(GreeterPartImpl) gets msg[" + req.getId() + " & " + req.getName() + "]");
    HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }
}