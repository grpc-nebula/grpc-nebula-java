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
import com.orientsec.grpc.server.CommonServiceServer;
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
 * 服务提供者取消注册
 *
 * @author sxp
 * @since 2018/7/21
 */
public class UnRegistryTest {
  private static final Logger logger = LoggerFactory.getLogger(UnRegistryTest.class);
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
   * 测试服务端注销
   *
   * @author sxp
   * @since 2018/12/1
   */
  @Test
  public void testUnRegistryService() throws Exception {
    // 启动服务
    CommonServiceServer server = new CommonServiceServer();
    server.start();
    String ip = IpUtils.getLocalHostAddress();
    String serviceName = server.getServiceName();
    int port = server.getPort();
    // 关闭服务
    server.stop();
    // 从zookeeper上获取注册数据
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(GlobalConstants.Consumer.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.PROVIDERS_CATEGORY);
    URL queryUrl = new URL(RegistryConstants.GRPC_PROTOCOL, ip, 0, parameters);
    ZkServiceImpl zkService = new ZkServiceImpl();
    List<URL> urls = zkService.lookup(queryUrl);
    // 校验查询到的数据
    if (urls == null || urls.size() == 0) {
      logger.info("停止服务之后服务注册信息被自动注销");
    } else {
      boolean find = false;
      String tempIp;
      int tempPort;
      for (URL url : urls) {
        tempIp = url.getHost();
        tempPort = url.getPort();
        if (ip.equals(tempIp) && port == tempPort) {
          find = true;
          break;
        }
      }
      Assert.assertFalse("停止服务之后，竟然还能查询到服务注册信息", find);
    }
  }
}
