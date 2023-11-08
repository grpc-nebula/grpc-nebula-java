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

public class KeepAvlieHelloWorldClient {
  private static final Logger logger = LoggerFactory.getLogger(KeepAvlieHelloWorldClient.class);

  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;

  public KeepAvlieHelloWorldClient() {
    String target = "zookeeper:///" + GreeterGrpc.SERVICE_NAME;

    channel = ManagedChannelBuilder.forTarget(target)
            // 在连接上没有发生服务调用时，是否允许发送HTTP/2 ping
            .keepAliveWithoutCalls(true)
            // 每隔多长时间发送一次HTTP/2 ping(最小值为10秒)
            .keepAliveTime(10, TimeUnit.MINUTES)
            // 发送HTTP/2 ping后等待服务端对ping进行响应结果的时间，超过这个时间则认为服务端不可用
            .keepAliveTimeout(10, TimeUnit.SECONDS)
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
    logger.info("Will try to greet " + name + " ...");
    HelloRequest request = HelloRequest.newBuilder().setId(id).setName(name).build();
    HelloReply response;
    try {
      // blockingStub = GreeterGrpc.newBlockingStub(channel).withDeadlineAfter(10, TimeUnit.SECONDS);
      if (id % 2 == 0) {
        response = blockingStub.sayHello(request);
      } else {
        response = blockingStub.echo(request);
      }

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
    logger.info("Greeting response: " + response.getMessage());
  }


  public static void main(String[] args) throws Exception {
    KeepAvlieHelloWorldClient client = new KeepAvlieHelloWorldClient();

    try {
      long count = 0;
      long interval = 5000L;// 时间单位为毫秒
      int id;
      long LOOP_NUM = 30 * 86400L * 1000 / interval;// 运行30天

      if (args.length > 0 && args[0].contains("high-concurrency")) {
        logger.info("Test high concurrency client ");
        interval = 10L;
        LOOP_NUM = 3600L * 1000 / interval;// 运行1小时
      }

      logger.info("Sleep interval is [" + interval + "] ms.");

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
      client.shutdown();
    }
  }
}
