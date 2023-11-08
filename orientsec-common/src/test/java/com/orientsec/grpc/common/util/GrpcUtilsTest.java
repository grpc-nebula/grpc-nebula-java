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

/**
 * 类描述
 *
 * @author sxp
 * @since 2018/11/28
 */
public class GrpcUtilsTest {
  @Test
  public void getInterfaceNameByFullMethodName() throws Exception {
    assertEquals("sxp.Hello", GrpcUtils.getInterfaceNameByFullMethodName("sxp.Hello/world"));
  }

  @Test
  public void getSimpleMethodName() throws Exception {
    assertEquals("world", GrpcUtils.getSimpleMethodName("sxp.Hello/world"));
  }

  @Test
  public void getInterfaceNameNoneException() throws Exception {
    assertEquals("", GrpcUtils.getInterfaceNameNoneException("sxp"));
  }

}
