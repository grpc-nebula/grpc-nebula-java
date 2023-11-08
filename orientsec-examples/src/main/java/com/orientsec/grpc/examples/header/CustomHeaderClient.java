/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orientsec.grpc.examples.header;

import com.orientsec.grpc.examples.helloworld.GreeterGrpc;
import com.orientsec.grpc.examples.helloworld.HelloReply;
import com.orientsec.grpc.examples.helloworld.HelloRequest;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * A simple client that like HelloWorldClient.
 * This client can help you create custom headers.
 */
public class CustomHeaderClient {
  private static final Logger logger = LoggerFactory.getLogger(CustomHeaderClient.class);

  private final ManagedChannel originChannel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;

  /**
   * A custom client.
   */
  private CustomHeaderClient(String host, int port) {
    String target = "zookeeper:///" + GreeterGrpc.SERVICE_NAME;
    originChannel = ManagedChannelBuilder
            //.forAddress(host, port)
            .forTarget(target)
            .usePlaintext()
            .build();
    ClientInterceptor interceptor = new HeaderClientInterceptor();
    Channel channel = ClientInterceptors.intercept(originChannel, interceptor);
    blockingStub = GreeterGrpc.newBlockingStub(channel);
  }

  private void shutdown() throws InterruptedException {
    originChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * A simple client method that like HelloWorldClient.
   */
  private void greet(String name) {
    logger.info("Will try to greet " + name + " ...");
    HelloRequest request = HelloRequest.newBuilder().setName(name).build();
    HelloReply response;
    try {
      response = blockingStub.sayHello(request);
    } catch (StatusRuntimeException e) {
      logger.warn("RPC failed: {}", e.getStatus());
      return;
    }
    logger.info("Greeting: " + response.getMessage());
  }

  /**
   * Main start the client from the command line.
   */
  public static void main(String[] args) throws Exception {
    CustomHeaderClient client = new CustomHeaderClient("localhost", 50051);
    try {
      /* Access a service running on the local machine on port 50051 */
      String user = "world";
      if (args.length > 0) {
        user = args[0]; /* Use the arg as the name to greet if provided */
      }

      int loopNum = 5;

      for (int i = 0; i < loopNum; i++) {
        client.greet(user + "-" + (i + 1));
      }

      logger.info("客户端调用服务端[" + loopNum + "]次，服务名为" + GreeterGrpc.SERVICE_NAME);

    } finally {
      client.shutdown();
    }
  }
}
