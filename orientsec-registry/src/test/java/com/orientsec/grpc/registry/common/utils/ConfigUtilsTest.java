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

/**
 * Test for ConfigUtils
 *
 * @author sxp
 * @since 2018/11/29
 */
public class ConfigUtilsTest {
  @Test
  public void isNotEmpty() throws Exception {
    String[] fileNames = {null, "", "false", "0", "null", "N/A"};
    for (String name : fileNames) {
      Assert.assertFalse(ConfigUtils.isNotEmpty(name));
    }
  }

  @Test
  public void isEmpty() throws Exception {
    String[] fileNames = {null, "", "false", "0", "null", "N/A"};
    for (String name : fileNames) {
      Assert.assertTrue(ConfigUtils.isEmpty(name));
    }
  }

  @Test
  public void getPid() throws Exception {
    Assert.assertTrue(ConfigUtils.getPid() >= 0);
  }

}
