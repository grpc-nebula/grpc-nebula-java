package com.orientsec.grpc.examples.helloworld;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreeterSleepImpl extends GreeterGrpc.GreeterImplBase {
    private static final Logger logger = LoggerFactory.getLogger(GreeterSleepImpl.class);

    private int sleepMilliseconds;

    public GreeterSleepImpl(int num) {
        this.sleepMilliseconds = num;
    }

    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
        System.out.println("server-(GreeterSleepImpl) gets msg[" + req.getId() + " & " + req.getName() + "]");
        try {
            Thread.sleep(sleepMilliseconds);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

}
