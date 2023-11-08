package com.orientsec.grpc.examples.helloworld;

import com.orientsec.grpc.examples.common.NameCreater;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ClientWithDeadline {

    private static final Logger logger = LoggerFactory.getLogger(LongTimeHelloClient.class);

    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    public ClientWithDeadline() {
        String target = "zookeeper:///" + GreeterGrpc.SERVICE_NAME;

        channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();

        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }


    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * 向服务端发送请求
     */
    public void greet(int id, String name) {
        logger.info("Send request...");
        HelloRequest request = HelloRequest.newBuilder().setId(id).setName(name).build();
        HelloReply response;
        try {
            response = blockingStub.withDeadlineAfter(1, TimeUnit.SECONDS).sayHello(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus() != null && Status.Code.DEADLINE_EXCEEDED.equals(e.getStatus().getCode())) {
                logger.error("服务调用超时", e);
            } else {
                logger.error("RPC failed:", e);
            }
            return;
        } catch (Exception e) {
            logger.error("RPC failed:", e);
            return;
        }
        logger.info("Get response: " + response.getMessage());
    }


    public static void main(String[] args) throws Exception {
        ClientWithDeadline client = new ClientWithDeadline();

        try {
            long count = 0;
            long interval = 1000L;// 时间单位为毫秒
            int id;
            long LOOP_NUM = 30 * 86400L * 1000 / interval;// 运行30天

            logger.info("Sleep interval is [" + interval + "] ms.");

            Random random = new Random();
            List<String> names = NameCreater.getNames();
            int size = names.size();

            while (true) {
                if (count++ >= LOOP_NUM) {
                    break;
                }

                id = random.nextInt(size);
                client.greet(id, names.get(id));

                TimeUnit.MILLISECONDS.sleep(interval);

                // 模拟客户端与服务端停止调用一段时间
                if (count > 0 && count % 10000 == 0) {
                    logger.info("Sleep " + 2 + " hours...");
                    TimeUnit.HOURS.sleep(2);
                }
            }
        } finally {
            client.shutdown();
        }
    }

}
