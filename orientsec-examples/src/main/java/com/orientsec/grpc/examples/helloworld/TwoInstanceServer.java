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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class TwoInstanceServer {
  private static final Logger logger = LoggerFactory.getLogger(TwoInstanceServer.class);

  private Server server1;
  private Server server2;

  private void start() throws Exception {
    final CountDownLatch serverLatch = new CountDownLatch(2);

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          server1 = ServerBuilder.forPort(50061)
                  .addService(new GreeterImpl())
                  .build()
                  .start();

          serverLatch.countDown();

          server1.awaitTermination();
        } catch (Exception e) {
          logger.error("server1:启动服务出错", e);
        }
      }
    }).start();

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          server2 = ServerBuilder.forPort(50062)
                  .addService(new GreeterImpl())
                  .build()
                  .start();

          serverLatch.countDown();

          server2.awaitTermination();
        } catch (Exception e) {
          logger.error("server1:启动服务出错", e);
        }
      }
    }).start();

    serverLatch.await();

  }


  /**
   * main
   *
   * @author sxp
   * @since 2019/8/2
   */
  public static void main(String[] args) throws Exception {
    final TwoInstanceServer server = new TwoInstanceServer();
    server.start();
  }



}
