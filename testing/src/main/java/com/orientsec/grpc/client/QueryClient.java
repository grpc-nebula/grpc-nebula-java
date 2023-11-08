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
import com.orientsec.grpc.service.QueryGrpc;
import com.orientsec.grpc.service.QueryReply;
import com.orientsec.grpc.service.QueryRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QueryClient {
  private static Logger logger = Logger.getLogger(QueryClient.class.getName());

  private final ManagedChannel channel;
  private final QueryGrpc.QueryBlockingStub blockingStub;

  public String getServiceName(){return QueryGrpc.SERVICE_NAME;}
  public QueryClient() {
    //channel = ManagedChannelBuilder.forAddress(host, port)
    //        .usePlaintext()
    //        .build();

    String target = "zookeeper:///" + QueryGrpc.SERVICE_NAME;

    channel = ManagedChannelBuilder.forTarget(target)
            .usePlaintext()
            .build();

    //blockingStub = GreeterGrpc.newBlockingStub(channel);
    blockingStub = QueryGrpc.newBlockingStub(channel).withCompression("gzip");
  }



  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  public void query(String name) {
    try {
      QueryRequest request = QueryRequest.newBuilder().setNo(10).setName(name).setAlias(name + "-alias")
    		  .setSex(true).setSalary(1.234f).setTotal(1234).setDesc("aaaa").setMisc("bbbb").build();
      QueryReply response = blockingStub.query(request);
      System.out.println(response.getMessage());
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
  }


  private static void runClient() throws InterruptedException {
	  QueryClient client = new QueryClient();

    long count = -1;
    long interval = 3000L;// 时间单位为毫秒
    long LOOP_NUM = 100000;

    while (true) {
      count++;
      if (count >= LOOP_NUM) {
        break;
      }

      client.query("world:" + System.currentTimeMillis());
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


}
