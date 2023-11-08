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
import java.util.concurrent.atomic.AtomicInteger;



/**
 * 服务流量控制——连接数控制
 *
 * @author huyuanning
 * @since 2018-7-27 modify by sxp 在测试代码中启动服务器
 */
public class ConnetionsControllerTest {
  private static Logger logger = LoggerFactory.getLogger(RequestsControllerTest.class);
  private static CommonServiceServer server = new CommonServiceServer();
  private static AtomicInteger errorCounter = new AtomicInteger(0);

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
    // 设置服务端最多接收10个连接
    ZkServiceImpl zkService = null;
    try {
      zkService = new ZkServiceImpl();
    } catch (PropertiesException e) {
      logger.error("获取zk的服务器地址出错", e);
    }
    URL url = getConnectionUrl();
    zkService.registerService(url);
    // -
    Thread.sleep(1000);
  }

  /**
   * 获取连接数控制的URL
   *
   * @author sxp
   * @since 2018/12/1
   */
  private static URL getConnectionUrl() {
    String serviceName = server.getServiceName();
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(RegistryConstants.DYNAMIC_KEY, "true");// 临时节点
    parameters.put(GlobalConstants.Consumer.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.CONFIGURATORS_CATEGORY);
    parameters.put(GlobalConstants.Provider.Key.DEFAULT_CONNECTION, "10");
    return new URL(RegistryConstants.OVERRIDE_PROTOCOL, "0.0.0.0", 0, parameters);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    server.stop();
    // -
    URL url = getConnectionUrl();
    new ZkServiceImpl().unRegisterService(url);
    TestProjectPropertyUtils.recoverUserDir();
  }

  /**
   * 连接数控制--正常情况
   * <p>
   * 服务端最大连接数为10，10个客户端去连接肯定不会报错。
   * <p/>
   *
   * @author huyuanlin
   * @since 2018/7/21
   * @since 2018-7-27 modify by sxp 使用CountDownLatch
   */
  @Test
  public void testNormalCondition() throws Exception {
    errorCounter.set(0);
    int THREAD_NUMBER = 10;
    final CountDownLatch clientLatch = new CountDownLatch(THREAD_NUMBER);
    GreeterClient[] clients = new GreeterClient[THREAD_NUMBER];
    for (int i = 0; i < THREAD_NUMBER; i++) {
      clients[i] = new GreeterClient();
      clientRequest(clients[i], 100 + i, clientLatch);
      Thread.sleep(20L);
    }
    try {
      for (int i = 0; i < THREAD_NUMBER; i++) {
        clients[i].shutdown();
      }
    } catch (InterruptedException e) {
      logger.error("关闭客户端出错", e);
    }
    // -
    clientLatch.await();
    // -
    Assert.assertEquals(errorCounter.get(), 0);
    TimeUnit.SECONDS.sleep(1L);
  }

  /**
   * 连接数控制--不正常情况
   * <p>
   * 服务端最大连接数为10，11个客户端去连接肯定会报错。
   * <p/>
   *
   * @author huyuanlin
   * @since 2018/7/21
   * @since 2018-7-27 modify by sxp 使用CountDownLatch
   */
  @Test
  public void testAbnormalCondition() throws Exception {
    errorCounter.set(0);
    // -
    int THREAD_NUMBER = 11;
    final CountDownLatch clientLatch = new CountDownLatch(THREAD_NUMBER);
    GreeterClient[] clients = new GreeterClient[THREAD_NUMBER];
    for (int i = 0; i < THREAD_NUMBER; i++) {
      clients[i] = new GreeterClient();
      clientRequest(clients[i], 200 + i, clientLatch);
      Thread.sleep(20L);
    }
    try {
      for (int i = 0; i < THREAD_NUMBER; i++) {
        clients[i].shutdown();
      }
    } catch (InterruptedException e) {
      logger.error("关闭客户端出错", e);
    }
    // -
    clientLatch.await();
    // -
    Assert.assertTrue("连接数流量控制预期结果不一致", errorCounter.get() > 0);
    TimeUnit.SECONDS.sleep(1L);
  }

  /**
   * 客户端发送请求
   *
   * @author sxp
   * @since 2018/12/1
   */
  private void clientRequest(final GreeterClient client, final int number, final CountDownLatch clientLatch) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        int INVOKE_TIMES = 2;
        try {
          for (int i = 0; i < INVOKE_TIMES; i++) {
            if (client.doSomethingForLongTime("hello:" + number + "-" + i) != 0) {
              errorCounter.incrementAndGet();
              break;
            }
            Thread.sleep(10L);
          }
        } catch (Throwable t) {
          logger.warn("客户端调用服务端出错", t);
        } finally {
          clientLatch.countDown();
        }
      }
    }).start();
  }
}
