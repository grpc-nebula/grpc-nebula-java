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
package com.orientsec.grpc.client;

import com.orientsec.grpc.service.GreeterGrpc;
import com.orientsec.grpc.service.GreeterRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestTimeoutClient {
  private static Logger logger = Logger.getLogger(TestTimeoutClient.class.getName());

  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;
  public static final long TIMEOUT_SECONDS = 5L;

  public TestTimeoutClient() {
    String target = "zookeeper:///" + GreeterGrpc.SERVICE_NAME;

    // 设置空闲超时时间为5秒
    channel = ManagedChannelBuilder.forTarget(target).idleTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .usePlaintext()
            .build();

    blockingStub = GreeterGrpc.newBlockingStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  public boolean greetIsSuccessful(String name) {
    try {
      GreeterRequest request = GreeterRequest.newBuilder().setName(name).build();
      blockingStub.sayHello(request);
      return true;
    } catch (Exception e) {
      logger.log(Level.INFO, e.getMessage(), e);
      return false;
    }
  }

   /**
   * main
   */
  public static void main(String[] args) throws Exception {
    TestTimeoutClient client = new TestTimeoutClient();

    try {
      logger.info("client greets: 1 --start--  ");
      client.greetIsSuccessful("world:" + System.currentTimeMillis());
      logger.info("client greets: 1 --end--  ");

      long newTimeOut = TIMEOUT_SECONDS + 1L;
      logger.info("Sleep " + newTimeOut + " seconds...   ");
      TimeUnit.SECONDS.sleep(newTimeOut);

      logger.info("client greets: 2 --start--   ");
      boolean successful = client.greetIsSuccessful("world:" + System.currentTimeMillis());
      logger.info("client greets: 2 --end--   ");

    } finally {
      client.shutdown();
    }

  }

  @Override
  public String toString() {
    return "TestTimeoutClient{" +
            "channel=" + channel +
            ", blockingStub=" + blockingStub +
            '}';
  }
}
