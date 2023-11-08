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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * grpc的健康监测API测试客户端
 * <p>
 * 实用性不大
 * </p>
 *
 * @author sxp
 * @since 2019/7/25
 */
public class HealthCheckClient {
  private static final Logger logger = LoggerFactory.getLogger(HealthCheckClient.class);

  private final ManagedChannel channel;
  private final HealthGrpc.HealthBlockingStub blockingStub;

  public HealthCheckClient() {
    String target = "zookeeper:///grpc.health.v1.Health";

    channel = ManagedChannelBuilder.forTarget(target)
            .usePlaintext()
            .build();

    blockingStub = HealthGrpc.newBlockingStub(channel);
  }


  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }


  public void check() {
    HealthCheckRequest request = HealthCheckRequest.newBuilder().setService(GreeterGrpc.SERVICE_NAME).build();
    HealthCheckResponse response;
    try {
        response = blockingStub.check(request);
    } catch (Exception e) {
      logger.error("RPC failed:", e);
      return;
    }
    logger.info("Greeting response: " + response.getStatus());
  }


  public static void main(String[] args) throws Exception {
    HealthCheckClient client = new HealthCheckClient();

    try {
      long count = 0;
      long interval = 5000L;// 时间单位为毫秒
      long LOOP_NUM = 100;

      logger.info("Sleep interval is [" + interval + "] ms.");

      while (true) {
        if (count++ >= LOOP_NUM) {
          break;
        }

        client.check();

        TimeUnit.MILLISECONDS.sleep(interval);
      }
    } finally {
      client.shutdown();
    }
  }
}
