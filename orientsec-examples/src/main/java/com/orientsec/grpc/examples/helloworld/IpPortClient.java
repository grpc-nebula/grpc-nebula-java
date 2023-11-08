package com.orientsec.grpc.examples.helloworld;

import com.orientsec.grpc.examples.common.NameCreater;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 使用IP、port的方式访问服务端
 *
 * @author sxp
 * @since 2019/10/29
 */
public class IpPortClient {
  private static final Logger logger = LoggerFactory.getLogger(HelloWorldClient.class);

  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;

  public IpPortClient(String host) {
    channel = ManagedChannelBuilder.forAddress(host, HelloWorldServer.port)
            .usePlaintext()
            .build();

    blockingStub = GreeterGrpc.newBlockingStub(channel);
  }


  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * 向服务端发送请求
   */
  public void greet(int id, String name) {
    logger.info("Will try to sayHello " + name + " ...");
    HelloRequest request = HelloRequest.newBuilder().setId(id).setName(name).build();
    HelloReply response;
    try {
        response = blockingStub.sayHello(request);
    } catch (StatusRuntimeException e) {
      if (e.getStatus() != null && Status.Code.DEADLINE_EXCEEDED.equals(e.getStatus().getCode())) {
        logger.error("服务调用超时", e);
      } else {
        logger.error("RPC failed:", e);
      }
      return;
    } catch (Exception e) {
      logger.error("RPC failed:", e);
      return;
    }
    logger.info("SayHello response: " + response.getMessage());
  }


  public static void main(String[] args) throws Exception {

    IpPortClient client = null;

    try {
      long count = 0;
      long interval = 5000L;// 时间单位为毫秒
      int id;
      long LOOP_NUM = 30 * 86400L * 1000 / interval;// 运行30天

      if (args == null || args.length == 0) {
        logger.error("请将服务端的IP地址作为第一个参数传递过来");
        return;
      }

      client = new IpPortClient(args[0]);

      Random random = new Random();
      List<String> names = NameCreater.getNames();
      int size = names.size();

      while (true) {
        if (count++ >= LOOP_NUM) {
          break;
        }

        id = random.nextInt(size);
        client.greet(id, names.get(id));

        TimeUnit.MILLISECONDS.sleep(interval);

        // 模拟客户端与服务端停止调用一段时间
        if (count > 0 && interval > 10 && count % 10000 == 0) {
          logger.info("Sleep " + 2 + " hours...");
          TimeUnit.HOURS.sleep(2);
        }
      }
    } finally {
      if (client != null) {
        client.shutdown();
      }
    }
  }

}
