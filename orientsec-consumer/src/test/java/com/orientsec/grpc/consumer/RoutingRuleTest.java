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
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.server.CommonServiceServer;
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



/**
 * 路由规则测试
 * <p>
 * 即黑、白名单测试
 * </p>
 *
 * @since 2018-8-8 modify by sxp 在程序中启动和关闭服务器
 */
public class RoutingRuleTest {
  private static Logger logger = LoggerFactory.getLogger(RoutingRuleTest.class);
  private static CommonServiceServer server = new CommonServiceServer();

  @BeforeClass
  public static void setUp() throws Exception {
    TestProjectPropertyUtils.setUserDir();

    final CountDownLatch serverLatch = new CountDownLatch(1);

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          server.start();
          serverLatch.countDown();
          server.blockUntilShutdown();
        } catch (Exception e) {
          logger.error("启动服务出错", e);
        }
      }
    }).start();

    serverLatch.await();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    server.stop();
    TestProjectPropertyUtils.recoverUserDir();
  }

  /**
   * 黑名单测试
   * <p>
   * 设定规则：rule=host = xxx.xxx.xxx.xxx =>
   * 使得特定IP(xxx.xxx.xxx.xxx)不能访问当前服务，当前测试用例设置本机IP不能访问服务。
   * </p>
   *
   * @author sxp
   * @since 2018-8-8
   */
  @Test
  public void testBlackList() throws Exception {
    String localIp = IpUtils.getIP4WithPriority();
    String rule = "host = " + localIp + " =>";
    URL url = createRouterUrl(rule);

    ZkServiceImpl zk = new ZkServiceImpl();
    zk.registerService(url);

    TimeUnit.SECONDS.sleep(1L);

    GreeterClient client = new GreeterClient();

    try {
      boolean successful = client.greetIsSuccessful("Hello");
      Assert.assertFalse("当前客户端处于黑名单，不能访问", successful);
    } finally {
      client.shutdown();
      zk.unRegisterService(url);
    }
  }

  /**
   * 客户端白名单测试
   * <p>
   * 设定规则：rule=host != 192.168.0.1 =>
   * 除了192.168.0.1，其它的客户端都不能服务端。
   * </p>
   *
   * @author sxp
   * @since 2018-8-8
   */
  @Test
  public void testConsumerWhiteList() throws Exception {
    final String EXCLUDED_IP = "192.168.0.1";

    String rule = "host != " + EXCLUDED_IP + " =>";
    URL url = createRouterUrl(rule);

    ZkServiceImpl zk = new ZkServiceImpl();
    zk.registerService(url);

    TimeUnit.SECONDS.sleep(1L);

    GreeterClient client = new GreeterClient();

    try {
      boolean successful = client.greetIsSuccessful("Hello");

      String localIp = IpUtils.getIP4WithPriority();

      if (localIp.equals(EXCLUDED_IP)) {
        Assert.assertTrue(successful);
      } else {
        Assert.assertFalse(successful);
      }

    } finally {
      client.shutdown();
      zk.unRegisterService(url);
    }
  }

  /**
   * 服务端白名单测试
   * <p>
   * 设定规则：rule= =>host!=xxx.xxx.xxx.xxx，所有客户端不能访问xxx.xxx.xxx.xxx上的服务
   * 当前测试用例设置本机IP的服务不能被访问。
   * </p>
   *
   * @author sxp
   * @since 2018-8-8
   */
  @Test
  public void testProviderWhiteList() throws Exception {
    String localIp = IpUtils.getIP4WithPriority();
    String rule = "=>host != " + localIp;
    URL url = createRouterUrl(rule);

    ZkServiceImpl zk = new ZkServiceImpl();
    zk.registerService(url);

    TimeUnit.SECONDS.sleep(1L);

    GreeterClient client = new GreeterClient();

    try {
      boolean successful = client.greetIsSuccessful("Hello");
      Assert.assertFalse("白名单测试", successful);
    } finally {
      client.shutdown();
      zk.unRegisterService(url);
    }
  }


  /**
   * 创建路由规则的URL
   *
   * @author sxp
   * @since 2018-8-8
   */
  private static URL createRouterUrl(String rule) {
    String interfaceName = server.getServiceName();

    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(GlobalConstants.CommonKey.INTERFACE, interfaceName);
    parameters.put(RegistryConstants.CATEGORY_KEY, RegistryConstants.ROUTERS_CATEGORY);
    parameters.put(RegistryConstants.DYNAMIC_KEY, "true");// 临时节点
    parameters.put(Constants.FORCE_KEY, "true");// 当路由结果为空时，是否强制执行
    parameters.put(GlobalConstants.NAME, "rule-" + System.currentTimeMillis());
    parameters.put(Constants.PRIORITY_KEY, String.valueOf(Integer.MAX_VALUE));// 优先级越大越靠前执行
    parameters.put("router", "condition");
    parameters.put(Constants.RULE_KEY, rule);
    parameters.put(Constants.RUNTIME_KEY, "false");

    return new URL(RegistryConstants.ROUTER_PROTOCOL, Constants.ANYHOST_VALUE, 0, parameters);
  }

}
