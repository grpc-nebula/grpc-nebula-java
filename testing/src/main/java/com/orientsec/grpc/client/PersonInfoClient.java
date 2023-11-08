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

import com.orientsec.grpc.service.PersonInfoGrpc;
import com.orientsec.grpc.service.PersonReply;
import com.orientsec.grpc.service.PersonRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 人员信息客户端
 *
 * @author sxp
 * @since 2018/8/21
 */
public class PersonInfoClient {
  private static Logger logger = Logger.getLogger(PersonInfoClient.class.getName());

  private final ManagedChannel channel;
  private final PersonInfoGrpc.PersonInfoBlockingStub blockingStub;

  public String getServiceName(){return PersonInfoGrpc.SERVICE_NAME;}

  public PersonInfoClient() {
    String target = "zookeeper:///" + PersonInfoGrpc.SERVICE_NAME;

    channel = ManagedChannelBuilder.forTarget(target)
            .usePlaintext()
            .build();

    blockingStub = PersonInfoGrpc.newBlockingStub(channel);
  }


  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * 查询人员信息
   */
  public PersonReply query(int no) {
    try {
      PersonRequest request = PersonRequest.newBuilder().setNo(no).build();
      PersonReply reply = blockingStub.query(request);
      return reply;
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      return null;
    }
  }

  public ManagedChannel getChannel() {
    return channel;
  }

  /**
   * main
   */
  public static void main(String[] args) throws InterruptedException {
    PersonInfoClient client = new PersonInfoClient();

    int count = -1;
    long interval = 1000L;// 时间单位为毫秒
    long LOOP_NUM = 1 * 86400 * 1000 / interval;// 运行1天

    PersonReply reply;
    StringBuilder sb = new StringBuilder();

    while (true) {
      count++;
      if (count >= LOOP_NUM) {
        break;
      }

      reply = client.query(count);

      if (count % 1000 == 1) {
        logger.info("客户端已经调用的次数为:" + count);

        sb.setLength(0);
        sb.append("name=[");
        sb.append(reply.getName());
        sb.append("],age=[");
        sb.append(reply.getAge());
        sb.append("],salary=[");
        sb.append(reply.getSalary());
        sb.append("]");

        logger.info(sb.toString());
      }

      TimeUnit.MILLISECONDS.sleep(interval);
    }

    client.shutdown();
  }
}
