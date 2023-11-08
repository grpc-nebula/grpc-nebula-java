package com.orientsec.grpc.examples.helloworld;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Greeter服务完整实现类
 *
 * @author sxp
 * @since 2019/6/18
 */
public class GreeterImpl extends GreeterGrpc.GreeterImplBase {
  private static final Logger logger = LoggerFactory.getLogger(GreeterImpl.class);

  @Override
  public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
    System.out.println("server gets msg[" + req.getId() + " & " + req.getName() + "]");
    HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).setNo(req.getId()).build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }

  @Override
  public void echo(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
    System.out.println("server gets msg[" + req.getId() + " & " + req.getName() + "]");
    HelloReply reply = HelloReply.newBuilder().setMessage(req.getName()).build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }

  @Override
  public void longTimeMethod(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
    System.out.println("server gets msg[" + req.getId() + " & " + req.getName() + "], sleep 5 seconds...");

    try {
      TimeUnit.SECONDS.sleep(5);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }


  private static AtomicLong lbCounter = new AtomicLong(0);

  @Override
  public void testLb(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
    long count = lbCounter.incrementAndGet();
    System.out.println("server's testLb method has been invoked [" + count + "] times.");

    HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }


  /**
   * 用于性能测试的一个方法
   * <p>
   * 主要逻辑：字符串拼接
   * </p>
   */
  @Override
  public void performanceTest(TestRequest req, StreamObserver<TestReply> responseObserver) {
    int id = req.getId();
    long timestamp = req.getTimestamp();
    boolean vip = req.getVip();
    String name = req.getName();
    float price = req.getPrice();
    double summation = req.getSummation();
    Map<Integer, String> args = req.getArgsMap();
    List<String> messages = req.getMessagesList();

    boolean success = vip;

    // 字符串拼接，作为返回结果
    StringBuilder sb = new StringBuilder();
    sb.append(id).append(timestamp).append(name).append(price).append(summation);

    Set<Map.Entry<Integer, String>> entrySet = args.entrySet();
    for (Map.Entry<Integer, String> entry : entrySet) {
      sb.append(entry.getKey()).append(entry.getValue());
    }

    for (String msg : messages) {
      sb.append(msg);
    }

    String message = sb.toString();

    TestReply reply = TestReply.newBuilder().setSuccess(success).setMessage(message).build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }

  @Override
  public void asynSayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
    System.out.println("server gets msg: id = " + req.getId());
    try {
      Random random = new Random();
      int duration = random.nextInt(2) + 1;
      TimeUnit.SECONDS.sleep(duration);
    } catch (InterruptedException e) {
      logger.error(e.getMessage(), e);
    }

    HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).setNo(req.getId()).build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }

  @Override
  public StreamObserver<TestRequest> asyncPerformanceTest(final StreamObserver<TestReply> responseObserver) {
    return new StreamObserver<TestRequest>() {
      String message = "";

      @Override
      public void onNext(TestRequest req) {
        int id = req.getId();
        long timestamp = req.getTimestamp();
        String name = req.getName();
        float price = req.getPrice();
        double summation = req.getSummation();
        Map<Integer, String> args = req.getArgsMap();
        List<String> messages = req.getMessagesList();

        StringBuilder sb = new StringBuilder();
        sb.append(id).append(timestamp).append(name).append(price).append(summation);

        Set<Map.Entry<Integer, String>> entrySet = args.entrySet();
        for (Map.Entry<Integer, String> entry : entrySet) {
          sb.append(entry.getKey()).append(entry.getValue());
        }

        for (String msg : messages) {
          sb.append(msg);
        }

        message = sb.toString();
        TestReply reply = TestReply.newBuilder().setSuccess(true).setMessage(message).build();
        responseObserver.onNext(reply);
      }

      @Override
      public void onError(Throwable t) {
        logger.warn("asyncPerformanceTest cancelled");
      }

      @Override
      public void onCompleted() {
        TestReply reply = TestReply.newBuilder().setSuccess(true).setMessage(message).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
      }
    };
  }

}