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

import com.orientsec.grpc.common.resource.SystemConfig;
import com.orientsec.grpc.common.util.PropertiesUtils;
import com.orientsec.grpc.examples.common.NameCreater;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class HelloWorldClient {
  private static final Logger logger = LoggerFactory.getLogger(HelloWorldClient.class);

  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;

  public HelloWorldClient() {
    //channel = ManagedChannelBuilder.forAddress(host, port)
    //    .usePlaintext()
    //    .build();

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
    long begin = System.currentTimeMillis();

    HelloRequest request = HelloRequest.newBuilder().setId(id).setName(name).build();
    HelloReply response;
    try {
      // blockingStub = GreeterGrpc.newBlockingStub(channel).withDeadlineAfter(10, TimeUnit.SECONDS);

      //if (id % 2 == 0) {
      //  response = blockingStub.sayHello(request);
      //} else {
      //  response = blockingStub.echo(request);
      //}

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

    long cost = System.currentTimeMillis() - begin;
    logger.info("耗时[" + cost + "]ms, Greeting response: " + response.getMessage());
  }

  public void echo(int id, String name) {
    long begin = System.currentTimeMillis();

    HelloRequest request = HelloRequest.newBuilder().setId(id).setName(name).build();
    HelloReply response;
    try {
      response = blockingStub.echo(request);
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

    long cost = System.currentTimeMillis() - begin;
    logger.info("耗时[" + cost + "]ms, Greeting response: " + response.getMessage());
  }


  public static void main(String[] args) throws Exception {
    HelloWorldClient client = new HelloWorldClient();

    try {
      long count = 0;
      long interval = getClientSleepInterval();// 单位：毫秒
      int id;
      long loopNum = 30 * 86400L * 1000;
      boolean isEcho = false;

      if (interval > 0) {
        loopNum = 30 * 86400L * 1000 / interval;// 运行30天
      }

      if (args.length > 0 && args[0].contains("high-concurrency")) {
        logger.info("Test high concurrency client ");
        interval = 10L;
        loopNum = 3600L * 1000 / interval;// 运行1小时
      }

      if (args.length > 0 && args[0].contains("echo-method")) {
        isEcho = true;
      }

      logger.info("Sleep interval is [" + interval + "] ms.");

      Random random = new Random();
      List<String> names = NameCreater.getNames();
      int size = names.size();

      while (true) {
        if (count++ >= loopNum) {
          break;
        }

        id = random.nextInt(size);
        if (isEcho) {
          client.echo(id, names.get(id));
        } else {
          client.greet(id, names.get(id));
        }

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


  private static long getClientSleepInterval() {
    String key = "client.sleep.interval.ms";
    long defaultValue = 200L;
    Properties properties = SystemConfig.getProperties();
    long value = PropertiesUtils.getValidLongValue(properties, key, defaultValue);
    if (value < 0L) {
      value = defaultValue;
    }

    logger.info(key + " = " + value);
    return value;
  }
}
