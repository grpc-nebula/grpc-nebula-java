package com.orientsec.grpc.examples.helloworld;

import com.orientsec.grpc.common.util.StringUtils;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 描述：按照概率使部分调用错误的Server
 *
 * @author zhuyujie
 * @since 2019-12-30
 */
public class ServerWithError {

    private static final Logger logger = LoggerFactory.getLogger(ServerWithError.class);

    private static int port = 50051;

    private static int percent = 50;

    private Server server;

    public ServerWithError(String percentString) {
        if (StringUtils.isEmpty(percentString)) {
            throw new RuntimeException("please enter a percent value (int) between 0 and 100 as an arg");
        }
        percent = Integer.parseInt(percentString);
        if (!(percent >= 0 && percent <= 100)) {
            throw new RuntimeException("percent is not between 0 and 100");
        }
        logger.info("percent is " + percentString);
    }

    public ServerWithError() {
    }

    private void start() throws IOException {

        server = ServerBuilder.forPort(port)
                .addService(new GreeterWithErrorImpl(percent))
                .build()
                .start();

        logger.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                ServerWithError.this.stop();
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
        try {
            if (args.length == 0) {
                throw new RuntimeException("please enter a percent value (int) between 0 and 100 as an arg");
            }
            final ServerWithError server = new ServerWithError(args[0]);
            server.start();
            server.blockUntilShutdown();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }


}
