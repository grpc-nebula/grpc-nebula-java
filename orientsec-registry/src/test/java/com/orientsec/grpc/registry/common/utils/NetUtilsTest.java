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
package com.orientsec.grpc.registry.common.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test for NetUtils
 *
 * @author sxp
 * @since 2018/11/30
 */
public class NetUtilsTest {
  private static final Logger logger = Logger.getLogger(NetUtilsTest.class.getName());
  private static final int MIN_RANDOM_PORT = 30000;
  private static final int MAX_RANDOM_PORT = 30000 + 10000 - 1;

  @Test
  public void getRandomPort() throws Exception {
    int port;

    for (int i = 0; i < 1500; i++) {
      port = NetUtils.getRandomPort();
      Assert.assertTrue(port >= MIN_RANDOM_PORT && port <= MAX_RANDOM_PORT);
    }
  }

  @Test
  public void getAvailablePort() throws Exception {
    ServerSocket ss = null;
    try {
      ss = new ServerSocket();
      ss.bind(null);
      int portInUse = ss.getLocalPort();

      int port;
      for (int i = 0; i < 100; i++) {
        port = NetUtils.getAvailablePort();
        Assert.assertTrue(port != portInUse);
      }

    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      if (ss != null) {
        try {
          ss.close();
        } catch (IOException e) {
        }
      }
    }

  }

  @Test
  public void getAvailablePortWithMinPort() throws Exception {
    ServerSocket ss = null;
    try {
      ss = new ServerSocket();
      ss.bind(null);
      int portInUse = ss.getLocalPort();

      int MIN_PORT = 20000;
      int port;

      for (int i = 0; i < 100; i++) {
        port = NetUtils.getAvailablePort(MIN_PORT);
        Assert.assertTrue(port != portInUse);
      }

    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    } finally {
      if (ss != null) {
        try {
          ss.close();
        } catch (IOException e) {
        }
      }
    }
  }


  @Test
  public void isValidAddress() throws Exception {
    Assert.assertTrue(NetUtils.isValidAddress("10.20.130.230:20880"));
    Assert.assertFalse(NetUtils.isValidAddress("10.20.130.230"));
    Assert.assertFalse(NetUtils.isValidAddress("10.20.130.230:666666"));
  }

  @Test
  public void isLocalHost() throws Exception {
    Assert.assertTrue(NetUtils.isLocalHost("127.0.0.1"));
    Assert.assertTrue(NetUtils.isLocalHost("127.0.00.1"));

    Assert.assertFalse(NetUtils.isLocalHost("192.168.0.1"));

    Assert.assertFalse(NetUtils.isLocalHost("127.0.0.1.a1"));
    Assert.assertFalse(NetUtils.isLocalHost("1.127.0.0.1"));
    Assert.assertFalse(NetUtils.isLocalHost("1.127.0.0.1.1"));

    Assert.assertTrue(NetUtils.isLocalHost("127.0.0.2"));
    Assert.assertTrue(NetUtils.isLocalHost("127.0.0.2"));
    Assert.assertTrue(NetUtils.isLocalHost("127.0.0.254"));
    Assert.assertTrue(NetUtils.isLocalHost("127.0.0.255"));
  }

  @Test
  public void isAnyHost() throws Exception {
    Assert.assertTrue(NetUtils.isAnyHost("0.0.0.0"));
    Assert.assertFalse(NetUtils.isAnyHost("192.0.0.0"));
  }

  @Test
  public void isInvalidLocalHost() throws Exception {
    Assert.assertTrue(NetUtils.isInvalidLocalHost(null));
    Assert.assertTrue(NetUtils.isInvalidLocalHost(""));
    Assert.assertTrue(NetUtils.isInvalidLocalHost("0.0.0.0"));
    Assert.assertTrue(NetUtils.isInvalidLocalHost("localhost"));
    Assert.assertTrue(NetUtils.isInvalidLocalHost("127.0.0.1"));
  }

  @Test
  public void isValidLocalHost() throws Exception {
    Assert.assertFalse(NetUtils.isValidLocalHost(null));
    Assert.assertFalse(NetUtils.isValidLocalHost(""));
    Assert.assertFalse(NetUtils.isValidLocalHost("0.0.0.0"));
    Assert.assertFalse(NetUtils.isValidLocalHost("localhost"));
    Assert.assertFalse(NetUtils.isValidLocalHost("127.0.0.1"));
  }

  @Test
  public void getLocalSocketAddress() throws Exception {
    InetSocketAddress add1 = NetUtils.getLocalSocketAddress("127.0.0.1", 11111);
    InetSocketAddress add2 = NetUtils.getLocalSocketAddress("localhost", 11111);
    InetSocketAddress add3 = NetUtils.getLocalSocketAddress("192.168.0.1", 11111);
    Assert.assertTrue(add1.equals(add2));
    Assert.assertTrue(!add1.equals(add3));
  }

  @Test
  public void getLocalHost() throws Exception {
    System.out.println(NetUtils.getLocalHost());
    Assert.assertFalse(NetUtils.getLocalHost().equals("127.0.0.1"));
  }

  @Test
  public void filterLocalHost() throws Exception {
    System.out.println(NetUtils.filterLocalHost("127.0.0.1:2222"));
    System.out.println(NetUtils.filterLocalHost("localhost:2222"));
    System.out.println(NetUtils.filterLocalHost("http://127.0.0.1"));
    System.out.println(NetUtils.filterLocalHost("http://127.0.0.1:2222"));
    System.out.println(NetUtils.filterLocalHost("http://localhost:2222"));
    System.out.println(NetUtils.filterLocalHost("http://192.168.0.1:1111"));
  }

  @Test
  public void getLocalAddress() throws Exception {
    InetAddress address = InetAddress.getByName("localhost");
    InetAddress localAddress = NetUtils.getLocalAddress();
    System.out.println(address);
    System.out.println(localAddress);
    Assert.assertFalse(address.equals(localAddress));
  }

  @Test
  public void getLogHost() throws Exception {
    String localHost = NetUtils.getLocalHost();
    System.out.println(localHost);
    Assert.assertFalse("127.0.0.1".equals(localHost));
  }

  @Test
  public void getHostName() throws Exception {
    Assert.assertEquals("localhost", NetUtils.getHostName("localhost"));
    Assert.assertEquals("127.0.0.1", NetUtils.getHostName("127.0.0.1"));
  }

  @Test
  public void getIpByHost() throws Exception {
    Assert.assertEquals("localhost", NetUtils.getHostName("localhost"));
    Assert.assertEquals("127.0.0.1", NetUtils.getIpByHost("localhost"));
    Assert.assertEquals("127.0.0.1", InetAddress.getByName("localhost").getHostAddress());
  }

  @Test
  public void toAddressString() throws Exception {
    InetSocketAddress address = new InetSocketAddress("localhost", 555);
    Assert.assertEquals("127.0.0.1:555", NetUtils.toAddressString(address));
  }

  @Test
  public void toAddress() throws Exception {
    Assert.assertEquals(new InetSocketAddress("localhost", 555), NetUtils.toAddress("localhost:555"));
    Assert.assertEquals(new InetSocketAddress("127.0.0.1", 555), NetUtils.toAddress("localhost:555"));
  }

  @Test
  public void toURL() throws Exception {
    String url = NetUtils.toURL("http", "192.168.0.1", 8888, "com.bocloud.Service");
    Assert.assertEquals("http://192.168.0.1:8888/com.bocloud.Service", url);
  }

}
