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

import org.junit.Assert;
import org.junit.Test;

/**
 * 类描述
 *
 * @author sxp
 * @since 2018/11/29
 */
public class UrlIpComparatorTest {
  @Test
  public void compare() throws Exception {
    UrlIpComparator demo = new UrlIpComparator();

    URL a = new URL("test", "192.168.0.1", 0);
    URL b = new URL("test", "192.168.0.7", 0);
    Assert.assertTrue(demo.compare(a, b) < 0);

    a = new URL("test", "192.168.0.1", 0);
    b = new URL("test", "0.0.0.0", 0);
    Assert.assertTrue(demo.compare(a, b) > 0);

    a = new URL("test", null, 0);
    b = new URL("test", null, 0);
    Assert.assertEquals(0, demo.compare(a, b));

    a = new URL("test", "192.168.0.1", 0);
    b = new URL("test", null, 0);
    Assert.assertTrue(demo.compare(a, b) > 0);

    a = new URL("test", null, 0);
    b = new URL("test", "192.168.0.7", 0);
    Assert.assertTrue(demo.compare(a, b) < 0);
  }

}
