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
package com.orientsec.grpc.registry.test;

import com.orientsec.grpc.client.GreeterClient;
import com.orientsec.grpc.common.TestProjectPropertyUtils;
import com.orientsec.grpc.common.ZkServiceImpl;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.util.IpUtils;
import com.orientsec.grpc.registry.common.URL;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 服务消费者注册
 *
 * @author sxp
 * @since 2018/11/29
 */
public class ConsumerRegistryTest {
  private static final Logger logger = Logger.getLogger(ConsumerRegistryTest.class.getName());

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
   */
  @Test
  public void testRegistryOneConsumer() throws Exception {
    // 启动服务
	final GreeterClient client = new GreeterClient();


    String ip = IpUtils.getLocalHostAddress();
    String serviceName = client.getServiceName();


    // 从zookeeper上获取注册数据
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(GlobalConstants.Consumer.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.CONSUMERS_CATEGORY);
    URL queryUrl = new URL(RegistryConstants.GRPC_PROTOCOL, ip, 0, parameters);

    ZkServiceImpl zkService = new ZkServiceImpl();
    List<URL> urls = zkService.lookup(queryUrl);

    // 校验查询到的数据
    Assert.assertNotNull(urls);
    Assert.assertNotEquals(0, urls.size());

    boolean find = false;
    String tempIp;

    for (URL url : urls) {
      tempIp = url.getHost();
      if (ip.equals(tempIp)) {
        find = true;
        logger.info("成功查询到SingleServiceConsumer的注册信息");
        break;
      }
    }

    if (!find) {
      Assert.fail("查询SingleServiceConsumer的注册信息失败");
    }

    //
    client.shutdown();
  }

  /**
   * 两个服务注册
   */
  @Test
  public void testRegistryTwoConsumers() throws Exception {
    // 启动服务
	GreeterClient[] clients = new GreeterClient[2];

	for(int i = 0; i < 2; i++){
		clients[i] = new GreeterClient();
		Thread.sleep(20L);
	}
    String ip = IpUtils.getLocalHostAddress();
    String serviceName = clients[0].getServiceName();

    // 从zookeeper上获取注册数据
    Map<String, String> parameters;
    URL queryUrl;
    boolean allFind = true, find;
    String tempIp;
    List<URL> urls;

	ZkServiceImpl zkService = new ZkServiceImpl();

	parameters = new HashMap<String, String>();
	parameters.put(GlobalConstants.Consumer.Key.INTERFACE, serviceName);
	parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.CONSUMERS_CATEGORY);
	queryUrl = new URL(RegistryConstants.GRPC_PROTOCOL, ip, 0, parameters);

	urls = zkService.lookup(queryUrl);

	// 校验查询到的数据
	Assert.assertNotNull(urls);
	Assert.assertNotEquals(0, urls.size());

	find = false;

	for (URL url : urls) {
		tempIp = url.getHost();

		if (ip.equals(tempIp)) {
			find = true;
			logger.info("成功查询到[" + serviceName + "]的consumer 注册信息");
			break;
		}
	}

	if (!find) {
		allFind = false;
		Assert.fail("查询TwoServiceConsumer的注册信息失败");
	}

	if (allFind) {
		logger.info("成功查询到TwoServiceConsumer的注册信息");
	}

	//
    for (int i = 0; i < 2; i++) {
      clients[i].shutdown();
    }
  }

  /**
   * 多个服务注册
   * <p>
   * 这个测试用例的目的是检测zookeeper一个节点下面是否能容纳20个子节点。
   * <p/>
   */
  @Test
  public void testRegistryManyConsumers() throws Exception {
    int NODE_NUM = 20;
    String SERVICE_NAME = "com.orientsec.sxp.test.service.Tester";
    String IP = IpUtils.getLocalHostAddress();

    Map<String, String> parameters;
    URL createUrl, queryUrl;
    int port;
    List<URL> urls;

    ZkServiceImpl zkService = new ZkServiceImpl();

    for (int index = 1; index <= NODE_NUM; index++) {
      // 写zookeeper
      parameters = new HashMap<String, String>();
      parameters.put(GlobalConstants.Consumer.Key.INTERFACE, SERVICE_NAME);
      parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.CONSUMERS_CATEGORY);
      port = index;
      createUrl = new URL(RegistryConstants.GRPC_PROTOCOL, IP, port, parameters);

      zkService.registerService(createUrl);
    }

    // 查询zookeeper
    parameters = new HashMap<String, String>();
    parameters.put(GlobalConstants.Consumer.Key.INTERFACE, SERVICE_NAME);
    parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.CONSUMERS_CATEGORY);
    queryUrl = new URL(RegistryConstants.GRPC_PROTOCOL, IP, 0, parameters);

    urls = zkService.lookup(queryUrl);

    // 校验查询到的数据
    Assert.assertNotNull(urls);
    Assert.assertEquals("检查是否向zookeeper注册[" + NODE_NUM + "]个子节点信息", NODE_NUM, urls.size());

    logger.info("成功向zookeeper注册[" + NODE_NUM + "]个子节点信息");

    //
    zkService.close();
  }

}
