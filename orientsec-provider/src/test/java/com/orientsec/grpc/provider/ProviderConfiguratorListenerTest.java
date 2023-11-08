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
import com.orientsec.grpc.provider.core.ServiceConfigUtils;
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

import static com.orientsec.grpc.common.constant.GlobalConstants.Provider.Key;

/**
 * 服务端配置信息监听测试
 *
 * @author sxp
 * @since 2018/8/16
 */
public class ProviderConfiguratorListenerTest {
  private static Logger logger = LoggerFactory.getLogger(ProviderConfiguratorListenerTest.class);
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
    // -
    new ZkServiceImpl().releaseRegistry();
    TestProjectPropertyUtils.recoverUserDir();
  }

  /**
   * 测试通用IP的0.0.0.0配置
   *
   * @author sxp
   * @since 2018-8-16
   */
  @Test
  public void testCommonIp() throws Exception {
    ZkServiceImpl zkService = new ZkServiceImpl();
    String serviceName = server.getServiceName();
    // -
    Map<String, Map<String, Object>> currentServicesConfig = ServiceConfigUtils.getCurrentServicesConfig();
    Map<String, Object> serviceConfig;
    boolean isProtected;

    if (currentServicesConfig.containsKey(serviceName)) {
      serviceConfig = currentServicesConfig.get(serviceName);
      isProtected = Boolean.valueOf(serviceConfig.get(Key.ACCESS_PROTECTED).toString()).booleanValue();
    } else {
      serviceConfig = null;
      isProtected = false;
    }
    // -
    Assert.assertTrue(serviceConfig != null);
    logger.info("配置前的[访问保护状态]为：" + isProtected);

    String configIp = "0.0.0.0";
    String configValue = isProtected ? "false" : "true";
    // -
    URL url = getUrl(serviceName, configIp, configValue);
    zkService.registerService(url);
    TimeUnit.SECONDS.sleep(1L);

    if (currentServicesConfig.containsKey(serviceName)) {
      serviceConfig = currentServicesConfig.get(serviceName);
      isProtected = Boolean.valueOf(serviceConfig.get(Key.ACCESS_PROTECTED).toString()).booleanValue();
    } else {
      serviceConfig = null;
      isProtected = false;
    }
    // -
    Assert.assertTrue(serviceConfig != null);
    logger.info("对IP为" + configIp + "提供者服务更新配置后的[访问保护状态]为：" + isProtected);
    Assert.assertTrue(Boolean.valueOf(configValue).booleanValue() == isProtected);
    // -
    zkService.unRegisterService(url);
    TimeUnit.SECONDS.sleep(1L);
  }

  /**
   * 测试指定IP的配置
   *
   * @author sxp
   * @since 2018-8-16
   */
  @Test
  public void testSpecifiedIp() throws Exception {
    ZkServiceImpl zkService = new ZkServiceImpl();
    String serviceName = server.getServiceName();
    // -
    Map<String, Map<String, Object>> currentServicesConfig = ServiceConfigUtils.getCurrentServicesConfig();
    Map<String, Object> serviceConfig;
    boolean isProtected;
    // -
    if (currentServicesConfig.containsKey(serviceName)) {
      serviceConfig = currentServicesConfig.get(serviceName);
      isProtected = Boolean.valueOf(serviceConfig.get(Key.ACCESS_PROTECTED).toString()).booleanValue();
    } else {
      serviceConfig = null;
      isProtected = false;
    }
    Assert.assertTrue(serviceConfig != null);
    logger.info("配置前的[访问保护状态]为：" + isProtected);
    // -
    String configIp = IpUtils.getIP4WithPriority();
    String configValue = isProtected ? "false" : "true";
    // -
    URL url = getUrl(serviceName, configIp, configValue);
    zkService.registerService(url);
    TimeUnit.SECONDS.sleep(1L);
    if (currentServicesConfig.containsKey(serviceName)) {
      serviceConfig = currentServicesConfig.get(serviceName);
      isProtected = Boolean.valueOf(serviceConfig.get(Key.ACCESS_PROTECTED).toString()).booleanValue();
    } else {
      serviceConfig = null;
      isProtected = false;
    }
    Assert.assertTrue(serviceConfig != null);
    logger.info("对IP为" + configIp + "提供者服务更新配置后的[访问保护状态]为：" + isProtected);
    Assert.assertTrue(Boolean.valueOf(configValue).booleanValue() == isProtected);
    zkService.unRegisterService(url);
    TimeUnit.SECONDS.sleep(1L);
    // 再测试指定一个非当前IP的配置，看看当前服务提供者属性是否变化
    String[] ips = {"192.168.0.1", "192.168.0.2"};
    String diffrentIp = null;
    boolean backupAcessProtected = isProtected;
    for (String ip : ips) {
      if (!ip.equals(IpUtils.getIP4WithPriority())) {
        diffrentIp = ip;
        break;
      }
    }
    Assert.assertTrue(diffrentIp != null);
    configIp = diffrentIp;
    configValue = isProtected ? "false" : "true";
    url = getUrl(serviceName, configIp, configValue);
    zkService.registerService(url);
    TimeUnit.SECONDS.sleep(1L);
    if (currentServicesConfig.containsKey(serviceName)) {
      serviceConfig = currentServicesConfig.get(serviceName);
      isProtected = Boolean.valueOf(serviceConfig.get(Key.ACCESS_PROTECTED).toString()).booleanValue();
    } else {
      serviceConfig = null;
      isProtected = false;
    }
    // -
    Assert.assertTrue(serviceConfig != null);
    logger.info("对IP为" + configIp + "(非服务提供者的IP)提供者服务更新配置后的[访问保护状态]为：" + isProtected);
    Assert.assertTrue(isProtected == backupAcessProtected);

    zkService.unRegisterService(url);
    TimeUnit.SECONDS.sleep(1L);
  }

  /**
   * 测试同时设置通用IP和指定IP的配置
   * <p>
   * 既有通用IP的配置，又有指定IP的配置，如果这两个配置冲突，以指定IP的配置为准。
   * </p>
   *
   * @author sxp
   * @since 2018-8-16
   */
  @Test
  public void testCommonAndSpecifiedIp() throws Exception {
    ZkServiceImpl zkService = new ZkServiceImpl();
    String serviceName = server.getServiceName();
    // -
    Map<String, Map<String, Object>> currentServicesConfig = ServiceConfigUtils.getCurrentServicesConfig();
    Map<String, Object> serviceConfig;
    boolean isProtected;
    if (currentServicesConfig.containsKey(serviceName)) {
      serviceConfig = currentServicesConfig.get(serviceName);
      isProtected = Boolean.valueOf(serviceConfig.get(Key.ACCESS_PROTECTED).toString()).booleanValue();
    } else {
      serviceConfig = null;
      isProtected = false;
    }
    Assert.assertTrue(serviceConfig != null);
    logger.info("配置前的[访问保护状态]为：" + isProtected);
    boolean backupAcessProtected = isProtected;
    // 指定IP配置
    String configIp = IpUtils.getIP4WithPriority();
    String configValue = backupAcessProtected ? "false" : "true";// 配置与backupAcessProtected相反的值
    URL specifiedUrl = getUrl(serviceName, configIp, configValue);
    zkService.registerService(specifiedUrl);
    // 通用IP配置
    configIp = "0.0.0.0";
    configValue = backupAcessProtected ? "true" : "false";;// 配置与backupAcessProtected相同的值
    URL commonUrl = getUrl(serviceName, configIp, configValue);
    zkService.registerService(commonUrl);
    // -
    TimeUnit.SECONDS.sleep(1L);
    // 获取当前属性值
    if (currentServicesConfig.containsKey(serviceName)) {
      serviceConfig = currentServicesConfig.get(serviceName);
      isProtected = Boolean.valueOf(serviceConfig.get(Key.ACCESS_PROTECTED).toString()).booleanValue();
    } else {
      serviceConfig = null;
      isProtected = false;
    }
    Assert.assertTrue(serviceConfig != null);
    logger.info("通用IP和指定IP同时配置，提供者服务更新配置后的[访问保护状态]为：" + isProtected);
    Assert.assertTrue(isProtected == !backupAcessProtected);
    // -
    zkService.unRegisterService(specifiedUrl);
    zkService.unRegisterService(commonUrl);
    TimeUnit.SECONDS.sleep(1L);
  }

  /**
   * 获取参数配置的URL
   *
   * @author sxp
   * @since 2018/12/1
   */
  private URL getUrl(String serviceName, String configIp, String configValue) {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(RegistryConstants.DYNAMIC_KEY, "true");// 临时节点
    parameters.put(GlobalConstants.Consumer.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.CONFIGURATORS_CATEGORY);
    parameters.put(Key.ACCESS_PROTECTED, configValue);
    return new URL(RegistryConstants.OVERRIDE_PROTOCOL, configIp, 0, parameters);
  }
}
