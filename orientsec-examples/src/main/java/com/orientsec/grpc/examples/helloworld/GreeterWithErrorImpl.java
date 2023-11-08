package com.orientsec.grpc.examples.helloworld;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：按照概率使部分调用错误
 *
 * @author zhuyujie
 * @since 2019-12-30
 */
public class GreeterWithErrorImpl extends GreeterGrpc.GreeterImplBase {

    private static final Logger logger = LoggerFactory.getLogger(GreeterImpl.class);

    private static Long sayHelloCount = 0L;

    private static Long sayHelloErrorCount = 0L;

    private static Long echoCount = 0L;

    private static Long echoErrorCount = 0L;

    //调用失败的概率
    private static int percent = 50;

    public GreeterWithErrorImpl(int percent) {
        GreeterWithErrorImpl.percent = percent;
    }

    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {

        int method = initMethod();

        if (sayHelloCount < 0) {
            sayHelloCount = 0L;
        }

        if (sayHelloErrorCount < 0) {
            sayHelloErrorCount = 0L;
        }

        System.out.println("server-(GreeterWithErrorImpl) gets msg[" + req.getId() + " & " + req.getName() + "]");
        if (method == 0) {

            //如果100/percent为整数，则每n次调用失败一次
            if (percent == 0) {
                HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).setNo(req.getId()).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } else {
                int n =  100 / percent;
                if (sayHelloCount % n == 0) {
                    HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).setNo(req.getId()).build();
                    responseObserver.onNext(reply);
                    responseObserver.onError(new RuntimeException("test error"));
                } else {
                    HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).setNo(req.getId()).build();
                    responseObserver.onNext(reply);
                    responseObserver.onCompleted();
                }
            }
        } else {
            //如果100/percent不是整数，则在errorRate大于percent时成功，小于时失败
            double errRate = sayHelloCount == 0 ? 0 : (double) sayHelloErrorCount / sayHelloCount * 100;

            if (errRate < percent) {
                sayHelloErrorCount++;
                //调用失败
                HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).setNo(req.getId()).build();
                responseObserver.onNext(reply);
                responseObserver.onError(new RuntimeException("test error"));
            } else {
                //调用成功
                HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).setNo(req.getId()).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            }
        }
        sayHelloCount++;
    }

    @Override
    public void echo(HelloRequest req, StreamObserver<HelloReply> responseObserver) {

        int method = initMethod();

        if (echoCount < 0) {
            echoCount = 0L;
        }

        if (echoErrorCount < 0) {
            echoErrorCount = 0L;
        }

        System.out.println("server-(GreeterWithErrorImpl) gets msg[" + req.getId() + " & " + req.getName() + "]");
        if (method == 0) {

            //如果100/percent为整数，则每n次调用失败一次
            if (percent == 0) {
                HelloReply reply = HelloReply.newBuilder().setMessage(req.getName()).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } else {
                int n =  100 / percent;
                if (echoCount % n == 0) {
                    HelloReply reply = HelloReply.newBuilder().setMessage(req.getName()).build();
                    responseObserver.onNext(reply);
                    responseObserver.onError(new RuntimeException("test error"));
                } else {
                    HelloReply reply = HelloReply.newBuilder().setMessage(req.getName()).build();
                    responseObserver.onNext(reply);
                    responseObserver.onCompleted();
                }
            }
        } else {
            //如果100/percent不是整数，则在errorRate大于percent时成功，小于时失败
            double errRate = echoCount == 0 ? 0 : (double) echoErrorCount / echoCount * 100;

            if (errRate < percent) {
                echoErrorCount++;
                //调用失败
                HelloReply reply = HelloReply.newBuilder().setMessage(req.getName()).build();
                responseObserver.onNext(reply);
                responseObserver.onError(new RuntimeException("test error"));
            } else {
                //调用成功
                HelloReply reply = HelloReply.newBuilder().setMessage(req.getName()).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            }
        }
        echoCount++;
    }

    private int initMethod() {
        if (percent == 0) {
            return 0;
        }

        int n = 100 / percent;
        if (n * percent == 100) {
            //如果100/percent为整数，则每n次调用失败一次
            return 0;
        } else {
            //如果100/percent不是整数
            return 1;
        }
    }
}
