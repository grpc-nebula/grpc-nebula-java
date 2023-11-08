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
import io.grpc.ServerServiceDefinition;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.services.HealthStatusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * grpc的健康监测API测试服务端
 * <p>
 * 实用性不大
 * </p>
 *
 * @author sxp
 * @since 2019/7/25
 */
public class HealthCheckServer {
  private static final Logger logger = LoggerFactory.getLogger(HealthCheckServer.class);

  private Server server;
  private HealthStatusManager manager = new HealthStatusManager();

  private void start() throws IOException {
    int port = 50051;

    server = ServerBuilder.forPort(port)
            .addService(manager.getHealthService())
            .addService(new GreeterImpl())
            .build()
            .start();

    List<ServerServiceDefinition> services = server.getServices();
    String serviceName;

    for (ServerServiceDefinition s : services) {
      serviceName = s.getServiceDescriptor().getName();
      manager.setStatus(serviceName, HealthCheckResponse.ServingStatus.SERVING);
    }

    logger.info("Server started, listening on " + port);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        HealthCheckServer.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() {
    if (server != null) {
      List<ServerServiceDefinition> services = server.getServices();
      String serviceName;

      for (ServerServiceDefinition s : services) {
        serviceName = s.getServiceDescriptor().getName();
        manager.clearStatus(serviceName);
      }

      server.shutdown();
    }
  }

  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    final HealthCheckServer server = new HealthCheckServer();
    server.start();
    server.blockUntilShutdown();
  }
}
