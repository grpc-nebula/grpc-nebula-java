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

import com.orientsec.grpc.service.PersonSalaryGrpc;
import com.orientsec.grpc.service.PersonReply;
import com.orientsec.grpc.service.PersonRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 人员薪水客户端
 *
 * @author sxp
 * @since 2018/8/18
 */
public class PersonSalaryClient {
  private static Logger logger = Logger.getLogger(PersonSalaryClient.class.getName());

  private final ManagedChannel channel;
  private final PersonSalaryGrpc.PersonSalaryBlockingStub blockingStub;

  public String getServiceName(){return PersonSalaryGrpc.SERVICE_NAME;}

  public PersonSalaryClient() {
    String target = "zookeeper:///" + PersonSalaryGrpc.SERVICE_NAME;

    channel = ManagedChannelBuilder.forTarget(target)
            .usePlaintext()
            .build();

    blockingStub = PersonSalaryGrpc.newBlockingStub(channel);
  }


  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * 查询人员薪水
   */
  public PersonReply querySalary(int no) {
    try {
      PersonRequest request = PersonRequest.newBuilder().setNo(no).build();
      PersonReply reply = blockingStub.querySalary(request);
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
    PersonSalaryClient client = new PersonSalaryClient();

    int count = -1;
    long interval = 3000L;// 时间单位为毫秒
    long LOOP_NUM = 10;
    PersonReply reply;

    while (true) {
      count++;
      if (count >= LOOP_NUM) {
        break;
      }

      reply = client.querySalary(count);

      logger.info("reply.salary=" + reply.getSalary());

      TimeUnit.MILLISECONDS.sleep(interval);
    }

    client.shutdown();
  }
}
