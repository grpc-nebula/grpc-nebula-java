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

public class TestLbClient {
  private static final Logger logger = LoggerFactory.getLogger(TestLbClient.class);

  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;

  public TestLbClient() {
    String target = "zookeeper:///" + GreeterGrpc.SERVICE_NAME;

    channel = ManagedChannelBuilder.forTarget(target)
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
    logger.info("Send request...");
    HelloRequest request = HelloRequest.newBuilder().setId(id).setName(name).build();
    HelloReply response;
    try {
        response = blockingStub.testLb(request);
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
    logger.info("Get response: " + response.getMessage());
  }


  public static void main(String[] args) throws Exception {
    TestLbClient client = new TestLbClient();

    try {
      long count = 0;
      long interval = 1000L;// 时间单位为毫秒
      int id;
      long LOOP_NUM = 30 * 86400L * 1000 / interval;// 运行30天

      logger.info("Sleep interval is [" + interval + "] ms.");

      Random random = new Random();
      List<String> names = NameCreater.getNames();
      int size = names.size();

      boolean isConsistentHash = false;
      if (args.length > 0 && args[0].contains("consistent_hash")) {
        logger.info("Test consistent_hash ");
        isConsistentHash = true;
      }

      while (true) {
        if (count++ >= LOOP_NUM) {
          break;
        }

        if (isConsistentHash) {
          id = size / 2;
        } else {
          id = random.nextInt(size);
        }
        client.greet(id, names.get(id));

        TimeUnit.MILLISECONDS.sleep(interval);

        // 模拟客户端与服务端停止调用一段时间
        if (count > 0 && count % 10000 == 0) {
          logger.info("Sleep " + 2 + " hours...");
          TimeUnit.HOURS.sleep(2);
        }
      }
    } finally {
      client.shutdown();
    }
  }
}
