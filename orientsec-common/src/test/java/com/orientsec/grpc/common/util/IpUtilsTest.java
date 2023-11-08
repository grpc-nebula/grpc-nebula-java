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
package com.orientsec.grpc.common.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for IpUtils
 *
 * @author sxp
 * @since 2018/11/28
 */
public class IpUtilsTest {
  @Test
  public void getIP() throws Exception {
    String ip1 = IpUtils.getIP4WithPriority();
    String ip2 = IpUtils.getLocalHostAddress();
    Assert.assertEquals(ip1, ip2);
    System.out.println("local address :" + ip1);
  }

  @Test
  public void isInnewerIP() throws Exception {
//    内网保留地址：
//    A类  10.0.0.0-10.255.255.255
//    B类  172.16.0.0-172.31.255.255
//    C类  192.168.0.0-192.168.255.255
//    特例 127.0.0.1
    Assert.assertFalse(IpUtils.isInnerIP("8.8.8.8"));
    Assert.assertFalse(IpUtils.isInnerIP("114.114.114.114"));

    Assert.assertTrue(IpUtils.isInnerIP("10.0.0.0"));
    Assert.assertTrue(IpUtils.isInnerIP("10.0.0.1"));
    Assert.assertTrue(IpUtils.isInnerIP("10.255.255.255"));

    Assert.assertTrue(IpUtils.isInnerIP("172.16.0.0"));
    Assert.assertTrue(IpUtils.isInnerIP("172.16.0.1"));
    Assert.assertTrue(IpUtils.isInnerIP("172.31.255.255"));

    Assert.assertTrue(IpUtils.isInnerIP("192.168.0.0"));
    Assert.assertTrue(IpUtils.isInnerIP("192.168.0.1"));
    Assert.assertTrue(IpUtils.isInnerIP("192.168.255.255"));

    Assert.assertTrue(IpUtils.isInnerIP("127.0.0.1"));
  }

  @Test
  public void getIpNum() throws Exception {
    long a = IpUtils.getIpNum("192.168.2.1");
    long b = IpUtils.getIpNum("192.168.1.254");
    Assert.assertTrue(a > b);
  }

  @Test
  public void isReachable() throws Exception {
    String ip = "127.0.0.1";
    Assert.assertTrue(IpUtils.isReachable(ip));
  }

}
