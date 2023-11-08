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
package com.orientsec.grpc.server;

import com.orientsec.grpc.common.Constants;
import com.orientsec.grpc.service.impl.PersonAgeImpl;
import com.orientsec.grpc.service.impl.PersonInfoImpl;
import com.orientsec.grpc.service.impl.PersonNameImpl;
import com.orientsec.grpc.service.impl.PersonSalaryImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * 人员服务的服务提供者
 *
 * @author sxp
 * @since 2018-8-18
 */
public class PersonServiceServer {
  private static Logger logger = Logger.getLogger(PersonServiceServer.class.getName());

  private Server server;
  private int port = Constants.Port.PERSON_SERVICE_SERVER;

  public void start() throws IOException {
    server = ServerBuilder.forPort(port)
            .addService(new PersonNameImpl())
            .addService(new PersonAgeImpl())
            .addService(new PersonSalaryImpl())
            .addService(new PersonInfoImpl())
            .build()
            .start();

    logger.info("PersonServiceServer start...");

    Runtime.getRuntime().addShutdownHook(new Thread() {

      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        PersonServiceServer.this.stop();
        System.err.println("*** PersonServiceServer shut down");
      }
    });
  }

  public void stop() {
    if (server != null) {
      logger.info("stop PersonServiceServer...");
      server.shutdown();
    }
  }

  // block 一直到退出程序
  public void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  public int getPort() {
    return port;
  }


  public static void main(String[] args) throws Exception {
    PersonServiceServer server = new PersonServiceServer();
    server.start();
    server.blockUntilShutdown();
  }

}
