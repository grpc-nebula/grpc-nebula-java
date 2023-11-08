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
import io.grpc.netty.NettyServerBuilder;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SingleThreadServer {
  private static final Logger logger = LoggerFactory.getLogger(SingleThreadServer.class);

  private Server server;

  private void start() throws IOException {
    int port = 50051;

    Executor executorPool = Executors.newFixedThreadPool(1,
            new DefaultThreadFactory("grpc-server-executor", true));

    EventLoopGroup workerEventLoopGroup = new NioEventLoopGroup(1,
            new DefaultThreadFactory("grpc-worker-group", true));

    //server = ServerBuilder.forPort(port)
    server = NettyServerBuilder.forPort(port)
            .executor(executorPool)// 自定义grpc服务端线程池
            .workerEventLoopGroup(workerEventLoopGroup)// 自定义netty的worker线程池
            .addService(new GreeterImpl())
            .build()
            .start();

    logger.info("Server started, listening on " + port);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        SingleThreadServer.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * main
   *
   * @author sxp
   * @since 2019/8/2
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    final SingleThreadServer server = new SingleThreadServer();
    server.start();
    server.blockUntilShutdown();
  }
}
