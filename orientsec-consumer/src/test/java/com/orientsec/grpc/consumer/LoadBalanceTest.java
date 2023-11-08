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
package com.orientsec.grpc.consumer;

import com.orientsec.grpc.client.GreeterClient;
import com.orientsec.grpc.common.TestProjectPropertyUtils;
import com.orientsec.grpc.common.ZkServiceImpl;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.util.IpUtils;
import com.orientsec.grpc.consumer.internal.ProvidersConfigUtils;
import com.orientsec.grpc.consumer.internal.ZookeeperNameResolver;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.server.CommonServiceSecondServer;
import com.orientsec.grpc.server.CommonServiceServer;
import io.grpc.NameResolver;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.orientsec.grpc.common.constant.GlobalConstants.Consumer;

/**
 * 负载均衡算法测试
 * <p>
 * 负载均衡策略，缺省值round_robin <br>
 * 可选值：pick_first、round_robin、weight_round_robin
 * </p>
 *
 * @author sxp
 * @since 2018/8/15
 */
public class LoadBalanceTest {
  private static Logger logger = LoggerFactory.getLogger(LoadBalanceTest.class);
  private static CommonServiceServer fisrtServer = new CommonServiceServer();
  private static CommonServiceSecondServer secondServer = new CommonServiceSecondServer();
  private static ZkServiceImpl zkService;
  private static URL setRequestModeUrl;

  @BeforeClass
  public static void setUp() throws Exception {
    TestProjectPropertyUtils.setUserDir();

    // 启动服务器
    final CountDownLatch serverLatch = new CountDownLatch(2);

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          fisrtServer.start();
          serverLatch.countDown();
          fisrtServer.blockUntilShutdown();
        } catch (Exception e) {
          logger.error("fisrtServer:启动服务出错", e);
        }
      }
    }).start();

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          secondServer.start();
          serverLatch.countDown();
          secondServer.blockUntilShutdown();
        } catch (Exception e) {
          logger.error("secondServer:启动服务出错", e);
        }
      }
    }).start();

    serverLatch.await();

    // 设置负载均衡模式为请求模式（这种模式下才会出现不断切换服务提供者的情况）
    zkService = new ZkServiceImpl();
    String serviceName = fisrtServer.getServiceName();
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(RegistryConstants.DYNAMIC_KEY, "true");// 临时节点
    parameters.put(Consumer.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.CONFIGURATORS_CATEGORY);
    parameters.put(Consumer.Key.LOADBALANCE_MODE_FOR_LISTENER, "request");
    setRequestModeUrl = new URL(RegistryConstants.OVERRIDE_PROTOCOL, "0.0.0.0", 0, parameters);
    zkService.registerService(setRequestModeUrl);

    TimeUnit.SECONDS.sleep(1L);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    fisrtServer.stop();
    secondServer.stop();
    if (zkService != null) {
      zkService.releaseRegistry();
    }

    TestProjectPropertyUtils.recoverUserDir();
  }

  /**
   * 测试round_robin算法
   * <p>
   * round_robin是轮询算法，在只有两个服务提供者的情况下，使用以下条件来判断是否为轮询算法：
   * (1)客户端共发起100次调用，两个服务提供者都被调用过
   * (2)两个服务提供者被调用的次数相同
   * </p>
   *
   * @author sxp
   * @since 2018-8-16
   */
  @Test
  public void testRoundRobin() throws Exception {
    String ALGORITHM = "round_robin";

    if (zkService == null) {
      zkService = new ZkServiceImpl();
    }
    URL url = getUrl(ALGORITHM);
    zkService.registerService(url);
    TimeUnit.SECONDS.sleep(1L);

    // 模拟客户端调用
    fisrtServer.getServiceImpl().setInvokedTimes(0);
    secondServer.getServiceImpl().setInvokedTimes(0);

    GreeterClient client = new GreeterClient();

    int LOOP_NUM = 100;

    try {
      for (int i = 0; i < LOOP_NUM; i++) {
        client.checkMethodInvoked();
      }
    } finally {
      client.shutdown();
    }

    int timesOfFirst = fisrtServer.getServiceImpl().getInvokedTimes();
    int timesOfSecond = secondServer.getServiceImpl().getInvokedTimes();
    logger.info(ALGORITHM + "负载均衡算法，各服务被调用次数分别为："
            + timesOfFirst + "," + timesOfSecond);

    Assert.assertTrue(timesOfFirst > 0);
    Assert.assertTrue(timesOfSecond > 0);
    Assert.assertTrue(timesOfFirst == timesOfSecond);

    zkService.unRegisterService(url);
    TimeUnit.SECONDS.sleep(1L);
  }


  /**
   * 测试weight_round_robin算法
   * <p>
   * weight_round_robin是一种随机算法，在只有两个服务提供者的情况下，使用以下条件来判断是否为随机算法：
   * (1)设置第二台服务器的权重400，第一台服务器的默认权重为100
   * (2)客户端共发起100次调用，两个服务提供者都被调用过
   * (3)两个服务提供者被调用的次数与权重的比例基本相同
   * </p>
   *
   * @author sxp
   * @since 2018-8-16
   */
  @Test
  public void testWeightRoundRobin() throws Exception {
    String ALGORITHM = "weight_round_robin";

    // 设置算法
    if (zkService == null) {
      zkService = new ZkServiceImpl();
    }
    URL url = getUrl(ALGORITHM);
    zkService.registerService(url);
    TimeUnit.SECONDS.sleep(1L);


    // 模拟客户端调用
    fisrtServer.getServiceImpl().setInvokedTimes(0);
    secondServer.getServiceImpl().setInvokedTimes(0);

    GreeterClient client = new GreeterClient();

    // 修改第二台服务器权重
    int newWeithg = 400;
    int defaultWeight = 100;
    double weightRatio = ((double) defaultWeight) / newWeithg;

    NameResolver nameResolver = client.getChannel().getNameResolver();
    ZookeeperNameResolver zookeeperNameResolver = (ZookeeperNameResolver) nameResolver;
    Map<String, ServiceProvider> services = zookeeperNameResolver.getAllProviders();
    Assert.assertTrue(services != null && services.size() == 2);

    String ip = IpUtils.getIP4WithPriority();
    int port = secondServer.getPort();
    String serviceName = secondServer.getServiceName();

    String providerIp;
    int providePort;

    for (Map.Entry<String, ServiceProvider> entry : services.entrySet()) {
      providerIp = entry.getValue().getHost();
      providePort = entry.getValue().getPort();
      if (providerIp.equals(ip) && providePort == port) {
        entry.getValue().setWeight(newWeithg);
        ProvidersConfigUtils.updateProperty(serviceName, providerIp, port, GlobalConstants.Provider.Key.WEIGHT, newWeithg);
        break;
      }
    }

    // 调用
    int LOOP_NUM = 100;

    try {
      for (int i = 0; i < LOOP_NUM; i++) {
        client.checkMethodInvoked();
      }
    } finally {
      client.shutdown();
    }

    int timesOfFirst = fisrtServer.getServiceImpl().getInvokedTimes();
    int timesOfSecond = secondServer.getServiceImpl().getInvokedTimes();
    double ratio = ((double) timesOfFirst) / timesOfSecond;


    logger.info(ALGORITHM + "负载均衡算法，各服务被调用次数分别为："
            + timesOfFirst + "," + timesOfSecond + "，两者比例为：" + ratio
            + "，权重比例为：" + weightRatio);

    Assert.assertTrue(timesOfFirst > 0);
    Assert.assertTrue(timesOfSecond > 0);
    Assert.assertTrue(Math.abs(ratio - weightRatio) <= 0.01);// 允许少量差异

    zkService.unRegisterService(url);
    TimeUnit.SECONDS.sleep(1L);

    // 恢复权重
    for (Map.Entry<String, ServiceProvider> entry : services.entrySet()) {
      providerIp = entry.getValue().getHost();
      providePort = entry.getValue().getPort();
      if (providerIp.equals(ip) && providePort == port) {
        entry.getValue().setWeight(defaultWeight);
        ProvidersConfigUtils.updateProperty(serviceName, providerIp, port, GlobalConstants.Provider.Key.WEIGHT, defaultWeight);
        break;
      }
    }
  }

  /**
   * 获取负载算法的URL对象
   *
   * @author sxp
   * @since 2018-8-16
   */
  private URL getUrl(String algorithm) {
    String serviceName = fisrtServer.getServiceName();
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(RegistryConstants.DYNAMIC_KEY, "true");// 临时节点
    parameters.put(Consumer.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.CONFIGURATORS_CATEGORY);
    parameters.put(GlobalConstants.CommonKey.DEFAULT_LOADBALANCE, algorithm);
    return new URL(RegistryConstants.OVERRIDE_PROTOCOL, "0.0.0.0", 0, parameters);
  }


}
