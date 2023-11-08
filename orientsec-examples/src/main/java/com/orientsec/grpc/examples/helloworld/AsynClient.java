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
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class AsynClient {
  private static final Logger logger = LoggerFactory.getLogger(AsynClient.class);

  private final ManagedChannel channel;
  private GreeterGrpc.GreeterStub asyncStub;

  private final StreamObserver<HelloReply> responseObserver = new StreamObserver<HelloReply>() {
    @Override
    public void onNext(HelloReply reply) {
      int no = reply.getNo();
      System.out.println("Get reponse: " + no);
    }

    @Override
    public void onError(Throwable t) {
      //logger.error("Error", t);
    }

    @Override
    public void onCompleted() {
      //logger.info("Completed.");
    }
  };

  public AsynClient() {
    String target = "zookeeper:///" + GreeterGrpc.SERVICE_NAME;

    channel = ManagedChannelBuilder.forTarget(target)
            .usePlaintext()
            .build();

    asyncStub = GreeterGrpc.newStub(channel);
  }


  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }


  public void asynSayHello(int id) {
    String name = "sxp";

    HelloRequest request = HelloRequest.newBuilder().setId(id).setName(name).build();

    try {
      asyncStub.asynSayHello(request, responseObserver);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }


  public static void main(String[] args) throws Exception {
    AsynClient client = new AsynClient();

    long count = 0;
    long interval = 5L;// 时间单位为毫秒
    long LOOP_NUM = 30L;
    int id;


    try {
      while (true) {
        if (count++ >= LOOP_NUM) {
          break;
        }

        id = (int) count;
        if (id <= 0) {
          logger.info("循环次数达到正整数的最大值");
          break;
        }

        client.asynSayHello(id);

        if (count == 1) {
          // 第一次请求客户端创建与服务端的连接需要花费一段时间
          TimeUnit.SECONDS.sleep(2);
        }

        TimeUnit.MILLISECONDS.sleep(interval);
      }
    } finally {
      TimeUnit.SECONDS.sleep((int) (LOOP_NUM * 2.5));
      client.shutdown();
    }
  }
}