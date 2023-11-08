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

import com.orientsec.grpc.common.util.MathUtils;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class HelloWorldServer {
  private static final Logger logger = LoggerFactory.getLogger(HelloWorldServer.class);

  public static int port = 50051;
  private Server server;


  private void start(Executor executor) throws IOException {
    server = ServerBuilder.forPort(port)
            .executor(executor)
            .addService(new GreeterImpl())
            .build()
            .start();

    logger.info("Server started, listening on " + port);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        HelloWorldServer.this.stop();
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
   * <pre>
   * HelloWorldServer set-thread-pool 1
   * HelloWorldServer set-port 50052
   * </pre>
   *
   * @author sxp
   * @since 2019/8/2
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    Executor executor = getExecutor(args);
    setPortByArgs(args);

    final HelloWorldServer server = new HelloWorldServer();
    server.start(executor);
    server.blockUntilShutdown();
  }


  /**
   * 如果有参数值指定了要自定义线程池，则根据参数值确定线程池大小并创建线程池
   */
  private static Executor getExecutor(String[] args) {
    Executor executor = null;

    if (args != null && args.length == 2 && "set-thread-pool".equals(args[0])) {
      String numStr = args[1];

      int nThreads = 0;
      if (MathUtils.isInteger(numStr)) {
        nThreads = Integer.parseInt(numStr);
      }
      if (nThreads == 0) {
        nThreads = Runtime.getRuntime().availableProcessors() * 2;
      }
      ThreadFactory factory = new DefaultThreadFactory("grpc-server-executor", true);
      executor = Executors.newFixedThreadPool(nThreads, factory);
    }

    return executor;
  }

  /**
   * 如果有参数值指定了服务端的端口，使用指定的端口更新默认端口
   */
  private static void setPortByArgs(String[] args) {
    if (args != null) {
      String numStr;

      if (args.length == 2 && "set-port".equals(args[0])) {
        numStr = args[1];
        setPort(numStr);
      } else if (args.length == 4 && "set-port".equals(args[2])) {
        numStr = args[3];
        setPort(numStr);
      }
    }
  }

  private static void setPort(String numStr) {
    int newPort = port;
    if (MathUtils.isInteger(numStr)) {
      newPort = Integer.parseInt(numStr);
    }

    port = newPort;
  }

}
