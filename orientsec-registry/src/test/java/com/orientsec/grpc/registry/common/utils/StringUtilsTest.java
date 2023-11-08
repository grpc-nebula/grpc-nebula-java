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

import com.orientsec.grpc.registry.common.Constants;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for StringUtils
 *
 * @author sxp
 * @since 2018/11/30
 */
public class StringUtilsTest {
  @Test
  public void isBlank() throws Exception {
    assertTrue(StringUtils.isBlank(null));
    assertTrue(StringUtils.isBlank(""));
    assertFalse(StringUtils.isBlank(" "));
    assertFalse(StringUtils.isBlank("sxp"));
  }

  @Test
  public void isEmpty() throws Exception {
    assertTrue(StringUtils.isEmpty(null));
    assertTrue(StringUtils.isEmpty(""));
    assertFalse(StringUtils.isEmpty(" "));
    assertFalse(StringUtils.isEmpty("sxp"));
  }

  @Test
  public void isNotEmpty() throws Exception {
    assertTrue(!StringUtils.isNotEmpty(null));
    assertTrue(!StringUtils.isNotEmpty(""));
    assertFalse(!StringUtils.isNotEmpty(" "));
    assertFalse(!StringUtils.isNotEmpty("sxp"));
  }

  @Test
  public void isInteger() throws Exception {
    assertTrue(StringUtils.isInteger("123"));
    assertTrue(StringUtils.isInteger("123456789"));
    //assertTrue(StringUtils.isInteger("12345678987654321"));

    assertFalse(StringUtils.isInteger("a123"));
    assertFalse(StringUtils.isInteger("abc"));
    assertFalse(StringUtils.isInteger("123abc"));
  }

  @Test
  public void parseInteger() throws Exception {
    assertEquals(0, StringUtils.parseInteger("a123"));
    assertEquals(0, StringUtils.parseInteger("abc"));
    assertEquals(0, StringUtils.parseInteger("123abc"));

    assertEquals(123, StringUtils.parseInteger("123"));
    assertEquals(123456789, StringUtils.parseInteger("123456789"));
    //assertEquals(12345678987654321L, StringUtils.parseInteger("12345678987654321"));
  }

  @Test
  public void testQueryString() throws Exception {
    Map<String, String> ps = new HashMap<String, String>();
    String qStr1 = StringUtils.toQueryString(ps);
    System.out.println(qStr1);
    Map<String, String> mapResult = StringUtils.parseQueryString(qStr1);

    assertEquals(ps, mapResult);

    ps.put("name", "sxp");
    ps.put("age", "18");
    ps.put("weight", "65");
    qStr1 = StringUtils.toQueryString(ps);
    System.out.println(qStr1);
    mapResult = StringUtils.parseQueryString(qStr1);

    assertEquals(ps, mapResult);
  }

  @Test
  public void getServiceKey() throws Exception {
    Map<String, String> ps = new HashMap<String, String>();
    ps.put(Constants.GROUP_KEY, "ServiceAAAAA");
    ps.put(Constants.INTERFACE_KEY, "com.bocloud.emp.NameService");
    ps.put(Constants.VERSION_KEY, "1.0.0");
    assertEquals("ServiceAAAAA/com.bocloud.emp.NameService:1.0.0", StringUtils.getServiceKey(ps));
  }

  @Test
  public void isEquals() throws Exception {
    assertTrue(StringUtils.isEquals(null, null));
    assertTrue(StringUtils.isEquals("", ""));
    assertTrue(StringUtils.isEquals("man", "man"));

    assertFalse(StringUtils.isEquals(null, ""));
    assertFalse(StringUtils.isEquals("man", ""));
    assertFalse(StringUtils.isEquals("man", "woman"));
  }

  @Test
  public void isContains() throws Exception {
    assertTrue(StringUtils.isContains("sxp,wf,xxx", "sxp"));
    assertTrue(StringUtils.isContains("sxp,wf,xxx", "xxx"));
    assertFalse(StringUtils.isContains("sxp,wf,xxx", "yyyyy"));
  }

  @Test
  public void isContains1() throws Exception {
    String[] names = {"sxp", "wf", "xxx"};
    assertTrue(StringUtils.isContains(names, "xxx"));
    assertFalse(StringUtils.isContains(names, "yyyy"));
  }

  @Test
  public void isNumeric() throws Exception {
    assertTrue(StringUtils.isNumeric("123"));
    assertTrue(StringUtils.isNumeric(""));// ???

    assertFalse(StringUtils.isNumeric(null));
    assertFalse(StringUtils.isNumeric("123.45"));
    assertFalse(StringUtils.isNumeric("a123.45"));
    assertFalse(StringUtils.isNumeric("abc"));
    assertFalse(StringUtils.isNumeric("abc.de"));
  }

}
