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

import com.orientsec.grpc.registry.common.URL;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Test for UrlUtils
 *
 * @author sxp
 * @since 2018/11/30
 */
public class UrlUtilsTest {
  @Test
  public void parseURL() throws Exception {
    URL url = UrlUtils.parseURL(null, null);
    assertEquals(null, url);

    url = UrlUtils.parseURL("192.168.1.1:2181,192.168.1.2:2181", null);
    assertEquals("grpc://192.168.1.1:2181?backup=192.168.1.2:2181", url.toFullString());

    Map<String, String> defaults = new HashMap<String, String>();
    defaults.put("protocol", "grpc");
    defaults.put("username", "sxp");
    defaults.put("password", "#$%");
    url = UrlUtils.parseURL("192.168.1.1:2181,192.168.1.2:2181", defaults);
    System.out.println(url.toFullString());
    assertEquals("grpc://sxp:#$%@192.168.1.1:2181?backup=192.168.1.2:2181", url.toFullString());
  }

  @Test
  public void parseURLs() throws Exception {
  }

  @Test
  public void convertRegister() throws Exception {
  }

  @Test
  public void convertSubscribe() throws Exception {
  }

  @Test
  public void revertRegister() throws Exception {
  }

  @Test
  public void revertSubscribe() throws Exception {
  }

  @Test
  public void revertNotify() throws Exception {
  }

  @Test
  public void revertForbid() throws Exception {
  }

  @Test
  public void getEmptyUrl() throws Exception {
  }

  @Test
  public void isMatchCategory() throws Exception {
  }

  @Test
  public void isMatch() throws Exception {
  }

  @Test
  public void isMatchGlobPattern() throws Exception {
  }

  @Test
  public void isMatchGlobPattern1() throws Exception {
  }

  @Test
  public void isServiceKeyMatch() throws Exception {
  }

  @Test
  public void isItemMatch() throws Exception {
  }

  @Test
  public void fromConfig() throws Exception {
  }

}
