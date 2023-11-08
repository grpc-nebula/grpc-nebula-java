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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 类描述
 *
 * @author sxp
 * @since 2018/11/28
 */
public class StringUtilsTest {
  @Test
  public void isEmpty() throws Exception {
    assertTrue(StringUtils.isEmpty(null));
    assertTrue(StringUtils.isEmpty(""));

    assertFalse(StringUtils.isEmpty(" "));

    assertFalse(StringUtils.isEmpty("sxp"));
    assertFalse(StringUtils.isEmpty("施小平"));
    assertFalse(StringUtils.isEmpty("x小p"));
  }

  @Test
  public void hasText() throws Exception {
    assertFalse(StringUtils.hasText(null));
    assertFalse(StringUtils.hasText(""));

    assertFalse(StringUtils.hasText(" "));

    assertTrue(StringUtils.hasText("sxp"));
    assertTrue(StringUtils.hasText("施小平"));
    assertTrue(StringUtils.hasText("x小p"));
  }

  @Test
  public void hasChineseCharacter() throws Exception {
    try {
      assertFalse(StringUtils.hasChineseCharacter(null));
      fail();
    } catch (Exception e) {
    }

    assertFalse(StringUtils.hasChineseCharacter(""));

    assertFalse(StringUtils.hasChineseCharacter(" "));

    assertFalse(StringUtils.hasChineseCharacter("sxp"));
    assertTrue(StringUtils.hasChineseCharacter("施小平"));
    assertTrue(StringUtils.hasChineseCharacter("x小p"));
  }

  @Test
  public void trim() throws Exception {
    assertEquals(null, StringUtils.trim(null));
    assertEquals("", StringUtils.trim(""));

    assertEquals("", StringUtils.trim(" "));

    assertEquals("sxp", StringUtils.trim("sxp"));
    assertEquals("施小平", StringUtils.trim("施小平"));
    assertEquals("x小p", StringUtils.trim("x小p"));

    assertEquals("sxp", StringUtils.trim(" sxp"));
    assertEquals("施小平", StringUtils.trim(" 施小平 "));
    assertEquals("x 小 p", StringUtils.trim("x 小 p"));
  }

}
