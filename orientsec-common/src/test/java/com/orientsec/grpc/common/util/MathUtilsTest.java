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
 * Test for MathUtils
 *
 * @author sxp
 * @since 2018/11/28
 */
public class MathUtilsTest {
  @Test
  public void isNumber() throws Exception {
    Assert.assertTrue(MathUtils.isNumber("121.57"));
    Assert.assertTrue(MathUtils.isNumber("121"));
    Assert.assertTrue(MathUtils.isNumber("999999999999"));

    Assert.assertFalse(MathUtils.isNumber("a12"));
    Assert.assertFalse(MathUtils.isNumber("0xFF"));
  }

  @Test
  public void isInteger() throws Exception {
    Assert.assertTrue(MathUtils.isInteger("121"));
    Assert.assertFalse(MathUtils.isInteger("999999999999"));
  }

  @Test
  public void isLong() throws Exception {
    Assert.assertTrue(MathUtils.isLong("121"));
    Assert.assertTrue(MathUtils.isLong("999999999999"));
  }

  @Test
  public void parseInt() throws Exception {
    Assert.assertEquals(123, MathUtils.parseInt("123"));

    Assert.assertEquals(0, MathUtils.parseInt("a123"));
    Assert.assertEquals(0, MathUtils.parseInt("999999999999"));
  }

  @Test
  public void parseLong() throws Exception {
    Assert.assertEquals(999999999999L, MathUtils.parseLong("999999999999"));
    Assert.assertEquals(123, MathUtils.parseLong("123"));

    Assert.assertEquals(0, MathUtils.parseLong("a123"));
    Assert.assertEquals(0, MathUtils.parseLong("0xFF"));
  }

  @Test
  public void round() throws Exception {
    double d = MathUtils.round(123.124, 2) - 123.12;
    Assert.assertTrue(Math.abs(d) < 0.000001);

    d = MathUtils.round(123.125, 2) - 123.13;
    Assert.assertTrue(Math.abs(d) < 0.000001);

    d = MathUtils.round(123.126, 2) - 123.13;
    Assert.assertTrue(Math.abs(d) < 0.000001);

    d = MathUtils.round(-123.124, 2) - (-123.12);
    Assert.assertTrue(Math.abs(d) < 0.000001);

    d = MathUtils.round(-123.125, 2) - (-123.13);
    Assert.assertTrue(Math.abs(d) < 0.000001);

    d = MathUtils.round(-123.126, 2) - (-123.13);
    Assert.assertTrue(Math.abs(d) < 0.000001);
  }

}
