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

import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.service.CommonReply;
import com.orientsec.grpc.service.CommonRequest;
import com.orientsec.grpc.service.GreeterGrpc;
import com.orientsec.grpc.service.GreeterReply;
import com.orientsec.grpc.service.GreeterRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GreeterClient {
  private static Logger logger = Logger.getLogger(GreeterClient.class.getName());

  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;

  public String getServiceName(){return GreeterGrpc.SERVICE_NAME;}
  public GreeterClient() {
    //channel = ManagedChannelBuilder.forAddress(host, port)
    //        .usePlaintext()
    //        .build();

    String target = "zookeeper:///" + GreeterGrpc.SERVICE_NAME;

    channel = ManagedChannelBuilder.forTarget(target)
            .usePlaintext()
            .build();

    //blockingStub = GreeterGrpc.newBlockingStub(channel);
    blockingStub = GreeterGrpc.newBlockingStub(channel).withCompression("gzip");
  }



  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  public void greet(String name) {
    try {
      GreeterRequest request = GreeterRequest.newBuilder().setName(name).build();
      GreeterReply response = blockingStub.sayHello(request);
      System.out.println(response.getMessage());
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
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

  //判断调用判断是否抛出异常
  public int doSomethingForLongTime(String name) {
    try {
      CommonRequest request = CommonRequest.newBuilder().setName(name).build();
      CommonReply response = blockingStub.doSomethingForLongTime(request);
      System.out.println(response.getMessage());
      return 0;
    } catch (Throwable t) {
      logger.log(Level.INFO, t.getMessage(), t);
      return -1;
    }
  }

  //调用Server，并返回Serverip及端口到
  public int doSomethingReturnServerInfo(String name) {
    try {
      CommonRequest request = CommonRequest.newBuilder().setName(name).build();
      CommonReply response  = blockingStub.doSomethingForLongTime(request);
      Map<String, ServiceProvider> serverMap = channel.getNameResolver().getServiceProviderMap();
      if(serverMap != null && serverMap.size()==1) {
        for (Map.Entry<String, ServiceProvider> entry : serverMap.entrySet()) {
           return entry.getValue().getPort();
        }
      }
    }
    catch ( Exception ex) {
      logger.log(Level.SEVERE, ex.getMessage(), ex);
    }
    return  0;
  }

  /**
   * 调用服务端的checkMethodInvoked方法
   *
   * @author sxp
   * @since 2018-8-8
   */
  public boolean checkMethodInvoked() {
    try {
      CommonRequest request = CommonRequest.newBuilder()
              .setNo(1)
              .setName("sxp")
              .setSex(true)
              .build();
      CommonReply response = blockingStub.checkMethodInvoked(request);

      boolean success = response.getSuccess();

      return success;
    } catch (Throwable t) {
      logger.log(Level.INFO, t.getMessage(), t);
      return false;
    }
  }


  private static void runClient() throws InterruptedException {
    GreeterClient client = new GreeterClient();

    long count = -1;
    long interval = 3000L;// 时间单位为毫秒
    long LOOP_NUM = 100000;

    while (true) {
      count++;
      if (count >= LOOP_NUM) {
        break;
      }

      client.greet("world:" + System.currentTimeMillis());
      logger.info("Sleep " + interval + " milliseconds...");
      TimeUnit.MILLISECONDS.sleep(interval);
    }

    client.shutdown();
  }

  /**
   * main
   */
  public static void main(String[] args) throws InterruptedException {
    runClient();
  }

  public ManagedChannel getChannel() {
    return channel;
  }
}
