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
package com.orientsec.grpc.provider;

import com.orientsec.grpc.common.TestProjectPropertyUtils;
import com.orientsec.grpc.common.ZkServiceImpl;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.util.IpUtils;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.server.SingleServiceServer;
import com.orientsec.grpc.server.TwoServicesServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 服务提供者注册
 *
 * @author sxp
 * @since 2018/7/12
 */
public class RegistryTest {
  private static final Logger logger = LoggerFactory.getLogger(RegistryTest.class);
  @BeforeClass
  public static void setUp() {
    TestProjectPropertyUtils.setUserDir();
  }
  @AfterClass
  public static void tearDown() throws Exception {
    new ZkServiceImpl().releaseRegistry();
    TestProjectPropertyUtils.recoverUserDir();
  }

  /**
   * 单一服务注册
   *
   * @author sxp
   * @since 2018-7-20
   */
  @Test
  public void testRegistryOneService() throws Exception {
    // 启动服务
    SingleServiceServer server = new SingleServiceServer();
    server.start();
    // -
    String ip = IpUtils.getLocalHostAddress();
    String serviceName = server.getServiceName();
    int port = server.getPort();
    // 从zookeeper上获取注册数据
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(GlobalConstants.Consumer.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.PROVIDERS_CATEGORY);
    URL queryUrl = new URL(RegistryConstants.GRPC_PROTOCOL, ip, 0, parameters);
    // -
    ZkServiceImpl zkService = new ZkServiceImpl();
    List<URL> urls = zkService.lookup(queryUrl);
    // 校验查询到的数据
    Assert.assertNotNull(urls);
    Assert.assertNotEquals(0, urls.size());
    boolean find = false;
    String tempIp;
    int tempPort;
    for (URL url : urls) {
      tempIp = url.getHost();
      tempPort = url.getPort();
      if (ip.equals(tempIp) && port == tempPort) {
        find = true;
        logger.info("成功查询到SingleServiceServer的注册信息");
        break;
      }
    }
    if (!find) {
      Assert.fail("查询SingleServiceServer的注册信息失败");
    }
    // -
    server.stop();
  }

  /**
   * 两个服务注册
   *
   * @author sxp
   * @since 2018-7-20
   */
  @Test
  public void testRegistryTwoServices() throws Exception {
    // 启动服务
    TwoServicesServer server = new TwoServicesServer();
    server.start();
    // -
    String ip = IpUtils.getLocalHostAddress();
    String[] serviceNames = server.getServiceNames();
    int port = server.getPort();
    // 从zookeeper上获取注册数据
    Map<String, String> parameters;
    URL queryUrl;
    boolean allFind = true, find;
    String tempIp;
    int tempPort;
    List<URL> urls;
    // -
    ZkServiceImpl zkService = new ZkServiceImpl();
    for (String serviceName : serviceNames) {
      parameters = new HashMap<String, String>();
      parameters.put(GlobalConstants.Consumer.Key.INTERFACE, serviceName);
      parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.PROVIDERS_CATEGORY);
      queryUrl = new URL(RegistryConstants.GRPC_PROTOCOL, ip, 0, parameters);
      urls = zkService.lookup(queryUrl);
      // 校验查询到的数据
      Assert.assertNotNull(urls);
      Assert.assertNotEquals(0, urls.size());
      find = false;
      for (URL url : urls) {
        tempIp = url.getHost();
        tempPort = url.getPort();
        if (ip.equals(tempIp) && port == tempPort) {
          find = true;
          logger.info("成功查询到[" + serviceName + "]的注册信息");
          break;
        }
      }
      if (!find) {
        allFind = false;
        Assert.fail("查询TwoServiceServer的注册信息失败");
      }
    }
    if (allFind) {
      logger.info("成功查询到TwoServiceServer的注册信息");
    }
    // -
    server.stop();
  }

  /**
   * 多个服务注册
   * <p>
   * 这个测试用例的目的是检测zookeeper一个节点下面是否能容纳20个子节点。
   * <p/>
   *
   * @author sxp
   * @since 2018-7-20
   */
  @Test
  public void testRegistryManyServices() throws Exception {
    int NODE_NUM = 20;
    String SERVICE_NAME = "com.orientsec.sxp.test.service.Tester";
    String IP = IpUtils.getLocalHostAddress();
    Map<String, String> parameters;
    URL createUrl, queryUrl;
    int port;
    List<URL> urls;
    ZkServiceImpl zkService = new ZkServiceImpl();
    // -
    for (int index = 1; index <= NODE_NUM; index++) {
      // 写zookeeper
      parameters = new HashMap<String, String>();
      parameters.put(GlobalConstants.Consumer.Key.INTERFACE, SERVICE_NAME);
      parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.PROVIDERS_CATEGORY);
      port = index;
      createUrl = new URL(RegistryConstants.GRPC_PROTOCOL, IP, port, parameters);

      zkService.registerService(createUrl);
    }
    // 查询zookeeper
    parameters = new HashMap<String, String>();
    parameters.put(GlobalConstants.Consumer.Key.INTERFACE, SERVICE_NAME);
    parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.PROVIDERS_CATEGORY);
    queryUrl = new URL(RegistryConstants.GRPC_PROTOCOL, IP, 0, parameters);
    urls = zkService.lookup(queryUrl);
    // 校验查询到的数据
    Assert.assertNotNull(urls);
    Assert.assertEquals("检查是否向zookeeper注册[" + NODE_NUM + "]个子节点信息", NODE_NUM, urls.size());
    logger.info("成功向zookeeper注册[" + NODE_NUM + "]个子节点信息");
    // -
    zkService.close();
  }
}
