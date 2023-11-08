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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TestPayClient {
  private static final Logger logger = LoggerFactory.getLogger(TestPayClient.class);

  private final ManagedChannel channel;
  private final TestPayGrpc.TestPayBlockingStub blockingStub;

  public TestPayClient() {
    String target = "zookeeper:///" + TestPayGrpc.SERVICE_NAME;

    channel = ManagedChannelBuilder.forTarget(target)
            .usePlaintext()
            .build();

    blockingStub = TestPayGrpc.newBlockingStub(channel);
  }


  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * 向服务端发送请求
   */
  public void pay(String name) {
    logger.info("Will try to consumerPay " + name + " ...");
    PayRequest request = PayRequest.newBuilder().setName(name).build();
    try {
      PayReply response = blockingStub.consumerPay(request);
      logger.info("TestPay response: " + response.getMessage());
    } catch (Exception e) {
      logger.error("RPC failed:", e);
    }
  }


  public static void main(String[] args) throws Exception {
    TestPayClient client = new TestPayClient();

    try {
      long count = 0;
      long interval = 5000L;// 时间单位为毫秒
      int id;
      long LOOP_NUM = 30 * 86400L * 1000 / interval;// 运行30天

      logger.info("Sleep interval is [" + interval + "] ms.");

      Random random = new Random();
      List<String> names = NameCreater.getNames();
      int size = names.size();

      while (true) {
        if (count++ >= LOOP_NUM) {
          break;
        }

        client.pay(String.valueOf(System.nanoTime()));

        TimeUnit.MILLISECONDS.sleep(interval);
      }
    } finally {
      client.shutdown();
    }
  }
}
