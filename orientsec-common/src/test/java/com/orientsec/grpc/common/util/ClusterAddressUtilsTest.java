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

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Test for ClusterAddressUtils
 *
 * @author sxp
 * @since 2018/11/28
 */
public class ClusterAddressUtilsTest {
  @Test
  public void getAddresses() throws Exception {
    String addressesString = "168.61.2.23:2181,168.61.2.24:2181,168.61.2.25:2181";
    List<InetSocketAddress> result = ClusterAddressUtils.getAddresses(addressesString);

    Assert.assertEquals("168.61.2.23", result.get(0).getHostName());
    Assert.assertEquals(2181, result.get(1).getPort());
    Assert.assertEquals("168.61.2.25", result.get(2).getHostName());
  }

}
