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
package com.orientsec.grpc.registry.common;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Test for URL
 *
 * @author sxp
 * @since 2018/11/29
 */
public class URLTest {
  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void valueOf() throws Exception {
    List<URL> urls = new ArrayList<URL>();
    urls.add(URL.valueOf("override://1.2.3.0/com.foo.BarService?category=configurators&dynamic=false&application=foo&weight=2"));
    urls.add(URL.valueOf("override://0.0.0.0/com.foo.BarService?category=configurators&dynamic=false&application=foo&weight=3"));
    urls.add(URL.valueOf("override://1.3.3.0/com.foo.BarService?category=configurators&dynamic=false&application=foo&weight=4"));
    Collections.sort(urls, new Comparator<URL>() {
      @Override
      public int compare(URL o1, URL o2) {
        return o1.getHost().compareTo(o2.getHost());
      }
    });
    Assert.assertEquals("0.0.0.0", urls.get(0).getHost());
    Assert.assertEquals("1.2.3.0", urls.get(1).getHost());
    Assert.assertEquals("1.3.3.0", urls.get(2).getHost());
  }

  @Test
  public void getProtocol() throws Exception {
  }

  @Test
  public void getUsername() throws Exception {
  }

  @Test
  public void getPassword() throws Exception {
  }

  @Test
  public void getAuthority() throws Exception {
  }

  @Test
  public void getIp() throws Exception {
    URL url = URL.valueOf("override://114.115.116.117:50001/com.foo.BarService?category=configurators&weight=2");
    Assert.assertEquals("114.115.116.117", url.getHost());
    Assert.assertEquals(url.getHost(), url.getIp());
  }

  @Test
  public void getPort() throws Exception {
    URL url = URL.valueOf("override://114.115.116.117:50001/com.foo.BarService?category=configurators&weight=2");
    Assert.assertEquals(50001, url.getPort());
  }

  @Test
  public void getPath() throws Exception {
    URL url = URL.valueOf("override://114.115.116.117:50001/com.foo.BarService?category=configurators&weight=2");
    Assert.assertEquals("com.foo.BarService", url.getPath());
  }

  @Test
  public void testProtocol() throws Exception {
    URL url = URL.valueOf("override://114.115.116.117:50001/com.foo.BarService?category=configurators&weight=2");
    URL sxpUrl = url.setProtocol("sxp");

    Assert.assertEquals("sxp", sxpUrl.getProtocol());
    Assert.assertEquals("override", url.getProtocol());
  }

  @Test
  public void testHost() throws Exception {
    URL url = URL.valueOf("override://114.115.116.117:50001/com.foo.BarService?category=configurators&weight=2");
    URL sxpUrl = url.setHost("8.9.10.11");

    Assert.assertEquals("8.9.10.11", sxpUrl.getHost());
    Assert.assertEquals("114.115.116.117", url.getHost());
  }

  @Test
  public void testPort() throws Exception {
    URL url = URL.valueOf("override://114.115.116.117:50001/com.foo.BarService?category=configurators&weight=2");
    URL sxpUrl = url.setPort(50002);

    Assert.assertEquals(50002, sxpUrl.getPort());
    Assert.assertEquals(50001, url.getPort());
  }

  @Test
  public void isLocalHost() throws Exception {
    URL url = URL.valueOf("grpc://127.0.0.1:50001/com.foo.BarService?category=configurators&weight=2");
    Assert.assertTrue(url.isLocalHost());

    url = URL.valueOf("grpc://localhost:50001/com.foo.BarService?category=configurators&weight=2");
    Assert.assertTrue(url.isLocalHost());
    Assert.assertEquals("127.0.0.1", url.getIp());

    url = URL.valueOf("grpc://192.168.1.3:50001/com.foo.BarService?category=configurators&weight=2");
    Assert.assertFalse(url.isLocalHost());
  }

  @Test
  public void isAnyHost() throws Exception {
    URL url = URL.valueOf("override://0.0.0.0:50001/com.foo.BarService?category=configurators&weight=2");
    Assert.assertTrue(url.isAnyHost());

    url = URL.valueOf("override://8.8.8.8:50001/com.foo.BarService?category=configurators&weight=2");
    Assert.assertFalse(url.isAnyHost());
  }
}
