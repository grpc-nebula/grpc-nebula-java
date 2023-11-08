package com.orientsec.grpc.examples.helloworld;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.model.BusinessResult;
import com.orientsec.grpc.common.util.IpUtils;
import com.orientsec.grpc.common.util.MapUtils;
import com.orientsec.grpc.provider.Registry;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.service.Provider;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 测试【应用不重启的情况下，注册新服务注册或更新已注册服务】
 *
 * @author sxp
 * @since 2019/7/10
 */
public class RegistryServiceTest {
  private static final Logger logger = LoggerFactory.getLogger(RegistryServiceTest.class);

  private Server server;

  private void start() throws IOException {
    int port = 50052;

    server = ServerBuilder.forPort(port)
            //.addService(new GreeterImpl())
            .addService(new TestPayImpl())
            .build()
            .start();

    logger.info("RegistryServiceTest started, listening on " + port);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        RegistryServiceTest.this.stop();
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

  public Server getServer() {
    return server;
  }

  /**
   * main
   *
   * @author sxp
   * @since 2019/7/10
   */
  public static void main(String[] args) throws Exception {
    final RegistryServiceTest testServer = new RegistryServiceTest();
    testServer.start();

    testRegistry(testServer);

    testServer.blockUntilShutdown();
  }

  private static void testRegistry(RegistryServiceTest testServer) throws Exception {
    testRegisterNewService(testServer);
    testUpdateService(testServer);
  }

  /**
   * 测试注册新服务
   *
   * @author sxp
   * @since 2019/7/10
   */
  private static void testRegisterNewService(RegistryServiceTest testServer) throws Exception {
    logger.info("sleep 15 seconds...");
    TimeUnit.SECONDS.sleep(15);
    logger.info("测试注册新服务...");

    String ip = IpUtils.getIP4WithPriority();
    Provider provider = new Provider();

    Map<String, String> parameters;
    URL queryUrl;
    List<URL> urls;

    Server server = testServer.getServer();
    int port = server.getPort();

    logger.info("开始注册服务...");

    // 构造入参ServerServiceDefinition
    GreeterGrpc.GreeterImplBase greeterImpl = new GreeterPartImpl();
    ServerServiceDefinition serviceDefinition = greeterImpl.bindService();

    // 调用注册新服务的接口
    BusinessResult result = Registry.registerNewService(server, serviceDefinition);

    String serviceName = serviceDefinition.getServiceDescriptor().getName();
    if (result.isSuccess()) {
      logger.info("注册新服务[" + serviceName + "]成功");
    } else {
      logger.info("注册新服务[" + serviceName + "]失败，" + result.getMessage());
    }

    if (result.isSuccess()) {
      parameters = new HashMap<>(MapUtils.capacity(2));
      parameters.put(GlobalConstants.Consumer.Key.INTERFACE, serviceName);
      parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.PROVIDERS_CATEGORY);
      queryUrl = new URL(RegistryConstants.GRPC_PROTOCOL, ip, port, parameters);
      urls = provider.lookup(queryUrl);
      logger.info("服务注册信息为[" + urls.get(0).toString() + "]");
    }

    logger.info("测试注册新服务: work done.");
  }

  /**
   * 测试更新服务
   *
   * @author sxp
   * @since 2019/7/10
   */
  private static void testUpdateService(RegistryServiceTest testServer) throws Exception {
    logger.info("sleep 15 seconds...");
    TimeUnit.SECONDS.sleep(15);
    logger.info("测试更新服务...");

    String ip = IpUtils.getIP4WithPriority();
    Provider provider = new Provider();

    Map<String, String> parameters;
    URL queryUrl;
    List<URL> urls;
    String serviceName;

    Server server = testServer.getServer();
    List<ServerServiceDefinition> services = server.getServices();
    int port = server.getPort();

    logger.info("开始更新服务...");
    BusinessResult result;

    // Greeter服务完整实现类
    GreeterGrpc.GreeterImplBase greeterImpl = new GreeterImpl();
    ServerServiceDefinition serviceDefinition = greeterImpl.bindService();

    serviceName = serviceDefinition.getServiceDescriptor().getName();
    parameters = new HashMap<>(MapUtils.capacity(2));
    parameters.put(GlobalConstants.Consumer.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.PROVIDERS_CATEGORY);
    queryUrl = new URL(RegistryConstants.GRPC_PROTOCOL, ip, port, parameters);
    urls = provider.lookup(queryUrl);

    // 调用更新服务的接口
    result = Registry.updateService(server, serviceDefinition);
    if (result.isSuccess()) {
      logger.info("更新服务[" + serviceName + "]成功");
    } else {
      logger.info("更新服务[" + serviceName + "]失败，" + result.getMessage());
    }

    if (result.isSuccess()) {
      logger.info("更新前的服务注册信息为[" + urls.get(0).toString() + "]");

      urls = provider.lookup(queryUrl);
      logger.info("更新后的服务注册信息为[" + urls.get(0).toString() + "]");
    }

    logger.info("更新服务: work done.");
  }


}
