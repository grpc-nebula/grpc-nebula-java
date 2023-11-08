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

/**
 * 测试【客户端空闲一段时间自动释放与服务端连接，再次发起调用时能自动恢复】
 *
 * @author sxp
 * @since 2019/2/1
 */
public class TestTimeoutClient {
  private static Logger logger = LoggerFactory.getLogger(TestTimeoutClient.class);

  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;

  public ManagedChannel getChannel() {
    return channel;
  }

  public TestTimeoutClient() {
    String target = "zookeeper:///" + GreeterGrpc.SERVICE_NAME;

    channel = ManagedChannelBuilder.forTarget(target)
            .idleTimeout(2, TimeUnit.MINUTES)
            .usePlaintext()
            .build();

    blockingStub = GreeterGrpc.newBlockingStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  public void greet(String name) {
    try {
      HelloRequest request = HelloRequest.newBuilder().setName(name).build();
      HelloReply response = blockingStub.sayHello(request);
      System.out.println("Get response: " + response.getMessage());
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

  private static void runClient() throws InterruptedException {
    TestTimeoutClient client = new TestTimeoutClient();

    try {
      Random random = new Random();
      List<String> names = NameCreater.getNames();
      int size = names.size();

      logger.info("client greets: 1 --start--  ");
      client.greet(names.get(random.nextInt(size)));
      logger.info("client greets: 1 --end--  ");

      logger.info("Sleep 3 minutes...   ");
      TimeUnit.MINUTES.sleep(3);

      logger.info("client greets: 2 --start--   ");
      client.greet(names.get(random.nextInt(size)));
      logger.info("client greets: 2 --end--   ");


      for (int i = 0; i < 5; i++) {
        client.greet(names.get(random.nextInt(size)));
      }
    } finally {
      TimeUnit.SECONDS.sleep(20L);
      client.shutdown();
    }

  }

  /**
   * main
   */
  public static void main(String[] args) throws InterruptedException {
    runClient();
  }
}
