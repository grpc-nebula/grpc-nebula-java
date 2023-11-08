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
import com.orientsec.grpc.common.enums.LoadBalanceMode;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.server.CommonServiceSecondServer;
import com.orientsec.grpc.server.CommonServiceServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;



import static com.orientsec.grpc.common.constant.GlobalConstants.Consumer;

/**
 * 负载均衡模式测试
 * <p>
 * 支持两种模式：一种是“请求负载均衡”，另一种是“连接负载均衡”。<br>
 * “请求负载均衡”指的是每次调用服务端都调用负载均衡算法选择一台服务器。<br>
 * “连接负载均衡”指的是，创建通道(Channel)后第一次调用选择服务器之后，一直复用与之前已选定的服务器建立的连接。<br>
 * 通过参数loadbalance.mode来进行控制。<br>
 * </p>
 *
 * @author sxp
 * @since 2018/11/29
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoadBalanceModeTest {
  private static Logger logger = LoggerFactory.getLogger(LoadBalanceModeTest.class);
  private static CommonServiceServer fisrtServer = new CommonServiceServer();
  private static CommonServiceSecondServer secondServer = new CommonServiceSecondServer();
  private static ZkServiceImpl zkService;


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
   * 测试连接负载均衡
   * <p>
   * 参数loadbalance.mode <br>
   * 类型string,缺省值connection,说明：负载均衡模式 <br>
   * 可选值为 connection 和 request,分别表示“连接负载均衡”、“请求负载均衡” <br>
   * </p>
   *
   * @author sxp
   * @since 2018-8-8
   */
  @Test
  public void testOrderBConnectionLoadBalanceMode() throws Exception {
    if (zkService == null) {
      zkService = new ZkServiceImpl();
    }
    URL url = getUrl(LoadBalanceMode.connection.name());
    zkService.registerService(url);
    TimeUnit.SECONDS.sleep(1L);

    fisrtServer.getServiceImpl().setInvoked(false);
    secondServer.getServiceImpl().setInvoked(false);

    GreeterClient client = new GreeterClient();

    int LOOP_NUM = 10;

    try {
      for (int i = 0; i < LOOP_NUM; i++) {
        client.checkMethodInvoked();
      }
    } finally {
      client.shutdown();
    }

    boolean atLeastOneInvoked = fisrtServer.getServiceImpl().isIsInvoked() || secondServer.getServiceImpl().isIsInvoked();
    boolean bothInvoked = fisrtServer.getServiceImpl().isIsInvoked() && secondServer.getServiceImpl().isIsInvoked();

    Assert.assertTrue(atLeastOneInvoked);
    Assert.assertFalse(bothInvoked);

    zkService.unRegisterService(url);
  }


  /**
   * 测试请求负载均衡
   * <p>
   * 参数loadbalance.mode <br>
   * 类型string,缺省值connection,说明：负载均衡模式 <br>
   * 可选值为 connection 和 request,分别表示“连接负载均衡”、“请求负载均衡” <br>
   * </p>
   *
   * @author sxp
   * @since 2018-8-8
   */
  @Test
  public void testOrderCRequestLoadBalanceMode() throws Exception {
    if (zkService == null) {
      zkService = new ZkServiceImpl();
    }
    URL url = getUrl(LoadBalanceMode.request.name());
    zkService.registerService(url);
    TimeUnit.SECONDS.sleep(1L);

    fisrtServer.getServiceImpl().setInvoked(false);
    secondServer.getServiceImpl().setInvoked(false);

    GreeterClient client = new GreeterClient();

    int LOOP_NUM = 10;

    try {
      for (int i = 0; i < LOOP_NUM; i++) {
        client.checkMethodInvoked();
      }
    } finally {
      client.shutdown();
    }

    boolean bothInvoked = fisrtServer.getServiceImpl().isIsInvoked() && secondServer.getServiceImpl().isIsInvoked();

    Assert.assertTrue(bothInvoked);

    zkService.unRegisterService(url);
  }


  /**
   * 获取设置负载均衡模式的URL
   *
   * @author sxp
   * @since 2018-8-17
   */
  private URL getUrl(String mode) {
    String serviceName = fisrtServer.getServiceName();
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(RegistryConstants.DYNAMIC_KEY, "true");// 临时节点
    parameters.put(Consumer.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.CONFIGURATORS_CATEGORY);
    parameters.put(Consumer.Key.LOADBALANCE_MODE_FOR_LISTENER, mode);
    return new URL(RegistryConstants.OVERRIDE_PROTOCOL, "0.0.0.0", 0, parameters);
  }
}
