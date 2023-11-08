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
package com.orientsec.grpc.common.resource;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.enums.LoadBalanceMode;
import com.orientsec.grpc.common.util.StringUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 类描述
 *
 * @author sxp
 * @since 2018/11/28
 */
public class SystemConfigTest {
  private static String oldUserDir = System.getProperty("user.dir");


  @BeforeClass
  public static void setUp() {
    System.setProperty("user.dir", oldUserDir + "/..");
  }

  @AfterClass
  public static void tearDown() {
    System.setProperty("user.dir", oldUserDir);
  }

  @Test
  public void getProperties() throws Exception {
    Properties pros = SystemConfig.getProperties();
    if (pros != null) {
      String retryNum = pros.getProperty("zookeeper.retryNum");
      Assert.assertEquals("5", retryNum);
    }
  }

  @Test
  public void getSwitchProperties() throws Exception {
    Properties pros = SystemConfig.getSwitchProperties();
    Assert.assertEquals(0, pros.size());
  }

  @Test
  public void getLoadBalanceMode() throws Exception {
    Map<String, String> loadBalanceModeMap = SystemConfig.getLoadBalanceModeMap();
    Assert.assertTrue(loadBalanceModeMap.size()  == 1);
    String mode = loadBalanceModeMap.get(GlobalConstants.LOAD_BALANCE_EMPTY_METHOD);
    Assert.assertTrue(StringUtils.isNotEmpty(mode));

    List<String> modeList = new ArrayList<String>(2);
    modeList.add(LoadBalanceMode.connection.name());
    modeList.add(LoadBalanceMode.request.name());

    Assert.assertTrue(modeList.contains(mode));
  }

  @Test
  public void testProviderMaxConnetions() throws Exception {
    int MAX = 10;
    SystemConfig.setProviderMaxConnetions(MAX);
    Assert.assertEquals(MAX, SystemConfig.getProviderMaxConnetions());
  }


}
