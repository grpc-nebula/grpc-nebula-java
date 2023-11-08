package com.orientsec.grpc.examples.helloworld;

import com.orientsec.grpc.common.util.MathUtils;
import com.orientsec.grpc.examples.common.NameCreater;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用于性能测试的客户端
 *
 * @author sxp
 * @since 2019/6/20
 */
public class PerformanceClient {
  private static final Logger logger = LoggerFactory.getLogger(PerformanceClient.class);

  /**
   * 异步调用客户端没发多少条请求，服务端才应答
   */
  public static final int ASYNC_REQUEST_NUM_ONCE = 100;

  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;
  private final GreeterGrpc.GreeterStub asyncStub;
  private final AtomicLong requestNum;


  public PerformanceClient(AtomicLong requestNum) {
    this.requestNum = requestNum;

    String target = "zookeeper:///" + GreeterGrpc.SERVICE_NAME;

    channel = ManagedChannelBuilder.forTarget(target)
            .usePlaintext()
            .build();

    blockingStub = GreeterGrpc.newBlockingStub(channel);
    asyncStub = GreeterGrpc.newStub(channel);
  }


  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * 向服务端发送请求
   */
  public void performanceTest(TestRequest request) {
    try {
      TestReply reply = blockingStub.performanceTest(request);
      boolean success = reply.getSuccess();
      String message = reply.getMessage();
    } catch (StatusRuntimeException e) {
      if (e.getStatus() != null && Status.Code.DEADLINE_EXCEEDED.equals(e.getStatus().getCode())) {
        System.out.println("服务调用超时");
      } else {
        System.out.println("RPC failed:" + e.getMessage());
        e.printStackTrace();
      }
      return;
    } catch (Exception e) {
      System.out.println("RPC failed:" + e.getMessage());
      e.printStackTrace();
      return;
    } finally {
      requestNum.incrementAndGet();
    }
  }

  public void asyncPerformanceTest(TestRequest request) {
    final CountDownLatch finishLatch = new CountDownLatch(1);

    StreamObserver<TestReply> responseObserver = new StreamObserver<TestReply>() {
      @Override
      public void onNext(TestReply reply) {
        boolean success = reply.getSuccess();
        String message = reply.getMessage();
      }

      @Override
      public void onError(Throwable t) {
        finishLatch.countDown();
        logger.warn("Failed: {0}", Status.fromThrowable(t));
      }

      @Override
      public void onCompleted() {
        finishLatch.countDown();
      }
    };

    StreamObserver<TestRequest> requestObserver = asyncStub.asyncPerformanceTest(responseObserver);
    try {
      for (int i = 0; i < ASYNC_REQUEST_NUM_ONCE; ++i) {
        requestObserver.onNext(request);
      }
    } catch (Exception e) {
      requestObserver.onError(e);
    } finally {
      requestObserver.onCompleted();
      requestNum.addAndGet(ASYNC_REQUEST_NUM_ONCE);
    }

    try {
      finishLatch.await();
    } catch (InterruptedException e) {
      logger.warn(e.getMessage(), e);
    }
  }


  public static void main(String[] args) throws Exception {
    boolean isSync = true;

    int len = args.length;
    if (len < 2) {
      System.out.println("至少传入两个参数:[线程个数] [每个线程运行的次数]");
      return;
    } else if (len == 3 && "async".equals(args[2])) {
      isSync = false;
    }

    String threadNumStr = args[0];
    String runTimeNumStr = args[1];

    if (!MathUtils.isInteger(threadNumStr)) {
      System.out.println("第一个参数[线程个数]必须为整数");
      return;
    }
    if (!MathUtils.isLong(runTimeNumStr)) {
      System.out.println("第二个参数[每个线程运行的次数]必须为整数");
      return;
    }

    int threadNum = Integer.parseInt(threadNumStr);
    final long runTimeNum = Long.parseLong(runTimeNumStr);

    if (threadNum <= 0 || runTimeNum <= 0) {
      System.out.println("[线程个数]、[每个线程运行的次数]必须为正整数");
      return;
    }

    if (isSync) {
      syncTest(threadNum, runTimeNum);
    } else {
      long num = runTimeNum / ASYNC_REQUEST_NUM_ONCE;
      if (num * ASYNC_REQUEST_NUM_ONCE < runTimeNum) {
        num++;
      }
      asyncTest(threadNum, num);
    }
  }

  /**
   * 同步接口性能测试
   *
   * @author sxp
   * @since 2019/8/5
   */
  private static void syncTest(int threadNum, final long runTimeNum) throws InterruptedException {
    final AtomicLong requestNum = new AtomicLong(0);

    // 多个客户端模拟多线程
    final PerformanceClient[] clients = new PerformanceClient[threadNum];
    for (int i = 0; i < threadNum; i++) {
      clients[i] = new PerformanceClient(requestNum);
      TimeUnit.MILLISECONDS.sleep(20);
    }

    final AtomicInteger index = new AtomicInteger(0);
    final CountDownLatch startCountDown = new CountDownLatch(1);

    ExecutorService threadPool = Executors.newFixedThreadPool(threadNum);

    for (int i = 0; i < threadNum; i++) {
      threadPool.execute(new Runnable() {
        @Override
        public void run() {
          int clientIndex = index.getAndIncrement();
          PerformanceClient client = clients[clientIndex];
          long costTime;
          Random random = new Random();

          List<String> names = NameCreater.getNames();
          int size = names.size();
          int id = random.nextInt(size);

          Map<Integer, String> map = new HashMap<>(3);
          map.put(id, names.get(id));
          map.put(id + 1, "map-" + names.get(id) + "-map");

          List<String> messages = new ArrayList<>(2);
          messages.add(names.get(id));
          messages.add("list-" + names.get(id) + "-list");

          TestRequest request = TestRequest.newBuilder()
                  .setId(id)
                  .setTimestamp(System.currentTimeMillis())
                  .setVip((id % 2 == 0) ? true : false)
                  .setName(names.get(id))
                  .setPrice(1.0F * id)
                  .setSummation(2.0D * id)
                  .putAllArgs(map)
                  .addAllMessages(messages)
                  .build();

          try {
            startCountDown.await();
          } catch (InterruptedException e) {
            logger.warn("StartCountDown await was interrupted.");
            Thread.currentThread().interrupt();
          }

          long startTime = System.currentTimeMillis();

          try {
            for (long j = 0; j < runTimeNum; j++) {
              if (j > 0 && j % 50000 == 0) {
                costTime = System.currentTimeMillis() - startTime;
                System.out.println("client[" + clientIndex + "], 已发送请求数 " + j + ", TPS:" + (j * 1000 / costTime));
              }

              client.performanceTest(request);
            }
          } finally {
            try {
              System.out.println("Shut down client[" + clientIndex + "]...");
              client.shutdown();
            } catch (InterruptedException e) {
              System.out.println("Client shutdown was interrupted.");
              Thread.currentThread().interrupt();
            }
          }
        }
      });
    }

    startCountDown.countDown();
    long begin = System.currentTimeMillis();

    threadPool.shutdown();

    try {
      while (!threadPool.isTerminated()) {
        TimeUnit.SECONDS.sleep(1);
      }
    } catch (InterruptedException e) {
      System.out.println("线程池关闭失败");
      threadPool.shutdownNow();
    }

    long end = System.currentTimeMillis();

    long spendTime = end - begin;
    long totalRequests = requestNum.get();
    double tpsValue = 1000D * totalRequests / spendTime; // 每秒的请求数
    long tps = (long) tpsValue;
    double responseTime = 1.0D * spendTime / runTimeNum;
    responseTime = MathUtils.round(responseTime, 2);

    System.out.println("线程数：" + threadNum + ", \n" +
            "总请求数：" + totalRequests + ", \n" +
            "总体TPS(每秒请求数): " + tps + ", \n" +
            "总时间:" + spendTime + " ms, \n" +
            "平均响应时间:" + responseTime + " ms.");
  }

  /**
   * 异步接口性能测试
   *
   * @author sxp
   * @since 2019/8/5
   */
  private static void asyncTest(int threadNum, final long runTimeNum) throws InterruptedException {
    final AtomicLong requestNum = new AtomicLong(0);

    // 多个客户端模拟多线程
    final PerformanceClient[] clients = new PerformanceClient[threadNum];
    for (int i = 0; i < threadNum; i++) {
      clients[i] = new PerformanceClient(requestNum);
      TimeUnit.MILLISECONDS.sleep(20);
    }

    final AtomicInteger index = new AtomicInteger(0);
    final CountDownLatch startCountDown = new CountDownLatch(1);

    ExecutorService threadPool = Executors.newFixedThreadPool(threadNum);

    for (int i = 0; i < threadNum; i++) {
      threadPool.execute(new Runnable() {
        @Override
        public void run() {
          int clientIndex = index.getAndIncrement();
          PerformanceClient client = clients[clientIndex];
          long costTime;
          Random random = new Random();

          List<String> names = NameCreater.getNames();
          int size = names.size();
          int id = random.nextInt(size);

          Map<Integer, String> map = new HashMap<>(3);
          map.put(id, names.get(id));
          map.put(id + 1, "map-" + names.get(id) + "-map");

          List<String> messages = new ArrayList<>(2);
          messages.add(names.get(id));
          messages.add("list-" + names.get(id) + "-list");

          TestRequest request = TestRequest.newBuilder()
                  .setId(id)
                  .setTimestamp(System.currentTimeMillis())
                  .setVip((id % 2 == 0) ? true : false)
                  .setName(names.get(id))
                  .setPrice(1.0F * id)
                  .setSummation(2.0D * id)
                  .putAllArgs(map)
                  .addAllMessages(messages)
                  .build();

          try {
            startCountDown.await();
          } catch (InterruptedException e) {
            logger.warn("StartCountDown await was interrupted.");
            Thread.currentThread().interrupt();
          }

          long startTime = System.currentTimeMillis();

          try {
            long requestNum;

            for (long j = 0; j < runTimeNum; j++) {
              requestNum = (j + 1) * ASYNC_REQUEST_NUM_ONCE;
              if (requestNum > 0 && requestNum % 50000 == 0) {
                costTime = System.currentTimeMillis() - startTime;
                System.out.println("client[" + clientIndex + "], 已发送请求数 " + requestNum + ", TPS:" + (requestNum * 1000 / costTime));
              }

              client.asyncPerformanceTest(request);
            }
          } finally {
            try {
              System.out.println("Shut down client[" + clientIndex + "]...");
              client.shutdown();
            } catch (InterruptedException e) {
              System.out.println("Client shutdown was interrupted.");
              Thread.currentThread().interrupt();
            }
          }
        }
      });
    }

    startCountDown.countDown();
    long begin = System.currentTimeMillis();

    threadPool.shutdown();

    try {
      while (!threadPool.isTerminated()) {
        TimeUnit.SECONDS.sleep(1);
      }
    } catch (InterruptedException e) {
      System.out.println("线程池关闭失败");
      threadPool.shutdownNow();
    }

    long end = System.currentTimeMillis();

    long spendTime = end - begin;
    long totalRequests = requestNum.get();
    double tpsValue = 1000D * totalRequests / spendTime; // 每秒的请求数
    long tps = (long) tpsValue;
    double responseTime = 1.0D * spendTime / (runTimeNum * ASYNC_REQUEST_NUM_ONCE);
    responseTime = MathUtils.round(responseTime, 2);

    System.out.println("线程数：" + threadNum + ", \n" +
            "总请求数：" + totalRequests + ", \n" +
            "总体TPS(每秒请求数): " + tps + ", \n" +
            "总时间:" + spendTime + " ms, \n" +
            "平均响应时间:" + responseTime + " ms.");
  }

}
