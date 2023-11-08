package com.orientsec.grpc.examples.helloworld;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HelloWorldServerSleep {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldServerSleep.class);

    public static int port = 50052;
    private Server server;

    private static int sleepMilliseconds = 0;

    private void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new GreeterSleepImpl(sleepMilliseconds))
                .build()
                .start();

        logger.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                HelloWorldServerSleep.this.stop();
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

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length > 0) {
            sleepMilliseconds = Integer.parseInt(args[0]);
        }
        final HelloWorldServerSleep server = new HelloWorldServerSleep();
        server.start();
        server.blockUntilShutdown();
    }

}
