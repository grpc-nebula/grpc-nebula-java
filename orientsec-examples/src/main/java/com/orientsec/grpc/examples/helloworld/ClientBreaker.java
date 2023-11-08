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

/**
 * 描述：
 *
 * @author zhuyujie
 * @since 2019-12-30
 */
public class ClientBreaker extends GreeterGrpc.GreeterImplBase {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldClient.class);

    private final ManagedChannel channel;

    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    private static Long errorCount = 0L;

    private static Long allCount = 0L;

    public ClientBreaker() {
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
        long begin = System.currentTimeMillis();
        HelloRequest request = HelloRequest.newBuilder().setId(id).setName(name).build();
        HelloReply response;
        try {
            allCount++;
            response = blockingStub.sayHello(request);
            long cost = System.currentTimeMillis() - begin;
            logger.info("耗时[" + cost + "]ms, Greeting response: " + response.getMessage());
        } catch (StatusRuntimeException e) {
            if (e.getStatus() != null && Status.Code.DEADLINE_EXCEEDED.equals(e.getStatus().getCode())) {
                logger.error("服务调用超时", e);
            } else {
                logger.error("RPC failed:", e);
            }
            errorCount++;
            return;
        } catch (Exception e) {
            logger.error("RPC failed:", e);
            errorCount++;
            return;
        } finally {
            logger.info("allCount:" + allCount);
            logger.info("errorCount:" + errorCount);
            logger.info("errorRate:" + 100 * (allCount == 0 ? 0 : (double) errorCount / allCount) + "%");
        }
    }

    public static void main(String[] args) throws Exception {
        ClientBreaker client = new ClientBreaker();

        try {
            long count = 0;
            long interval = 1000L;// 单位：毫秒
            int id;
            long loopNum = 30 * 86400L * 1000;

            logger.info("Sleep interval is [" + interval + "] ms.");

            Random random = new Random();
            List<String> names = NameCreater.getNames();
            int size = names.size();

            while (true) {
                if (count++ >= loopNum) {
                    break;
                }

                id = random.nextInt(size);
                client.greet(id, names.get(id));

                TimeUnit.MILLISECONDS.sleep(interval);

                // 模拟客户端与服务端停止调用一段时间
                if (count > 0 && interval > 10 && count % 10000 == 0) {
                    logger.info("Sleep " + 2 + " hours...");
                    TimeUnit.HOURS.sleep(2);
                }
            }
        } finally {
            client.shutdown();
        }
    }


}
