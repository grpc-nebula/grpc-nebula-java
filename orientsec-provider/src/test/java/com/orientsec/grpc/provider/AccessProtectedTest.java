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

import com.orientsec.grpc.client.GreeterClient;
import com.orientsec.grpc.common.TestProjectPropertyUtils;
import com.orientsec.grpc.common.ZkServiceImpl;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.exception.PropertiesException;
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
 * 服务访问保护
 * <p>
 * 每个服务端可以配置默认情况下是否开放给客户端：即某些服务端默认禁止访问，另外一些服务端默认开放访问。<br>
 * 每个服务端可以配置access.protected参数，控制默认情况下是否开放给客户端。
 * </p>
 *
 * @author sxp
 */
public class AccessProtectedTest {
  private static Logger logger = LoggerFactory.getLogger(AccessProtectedTest.class);
  private static CommonServiceServer server = new CommonServiceServer();

  @BeforeClass
  public static void setUp() throws Exception {
    TestProjectPropertyUtils.setUserDir();
    // 启动服务器
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
   * 设置访问保护状态为true，即某些服务端默认禁止访问。
   * 设置访问保护状态为false，可以恢复访问。
   *
   * @author sxp
   * @since  2017-8-8
   */
  @Test
  public void test() throws Exception {
    // 设置访问保护状态为true，即某些服务端默认禁止访问
    ZkServiceImpl zkService = null;
    try {
      zkService = new ZkServiceImpl();
    } catch (PropertiesException e) {
      logger.error("获取zk的服务器地址出错", e);
    }
    String serviceName = server.getServiceName();
    GreeterClient client = new GreeterClient();
    // 设置为保护状态
    URL url = getUrl(serviceName, "true");
    zkService.registerService(url);
    TimeUnit.SECONDS.sleep(1L);
    boolean successful = client.greetIsSuccessful("Hello");
    Assert.assertFalse(successful);
    zkService.unRegisterService(url);
    // 取消保护状态
    url = getUrl(serviceName, "false");
    zkService.registerService(url);
    TimeUnit.SECONDS.sleep(2L);
    successful = client.greetIsSuccessful("Hello");
    Assert.assertTrue(successful);
    zkService.unRegisterService(url);
    client.shutdown();
  }

  /**
   * 获取访问保护控制的URL
   *
   * @author sxp
   * @since 2018/12/1
   */
  private URL getUrl(String serviceName, String accessProtected) {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(RegistryConstants.DYNAMIC_KEY, "true");// 临时节点
    parameters.put(GlobalConstants.Consumer.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.CONFIGURATORS_CATEGORY);
    parameters.put(GlobalConstants.Provider.Key.ACCESS_PROTECTED, accessProtected);
    return new URL(RegistryConstants.OVERRIDE_PROTOCOL, "0.0.0.0", 0, parameters);
  }
}
