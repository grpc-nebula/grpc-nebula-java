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
package com.orientsec.grpc.common.exception;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for BusinessException
 *
 * @author sxp
 * @since 2018/11/28
 */
public class BusinessExceptionTest {
  @Test
  public void testConstructor() throws Exception {
    BusinessException instance = new BusinessException();
    Assert.assertEquals(null, instance.getMessage());

    Throwable cause = new Throwable("test throwable");
    String msg = "test error text";
    instance = new BusinessException(msg, cause);
    Assert.assertEquals(msg, instance.getMessage());

    instance = new BusinessException(cause);
    Assert.assertEquals(cause, instance.getCause());

    int errorCode = 100;
    instance = new BusinessException(errorCode, msg, cause);
    Assert.assertEquals(errorCode, instance.getErrorCode());
    Assert.assertEquals(msg, instance.getMessage());
    Assert.assertEquals(cause, instance.getCause());
  }

  @Test
  public void testSetAndGetErrorCode() throws Exception {
    BusinessException instance = new BusinessException("test error text.");

    int errorCode = 100;
    instance.setErrorCode(errorCode);
    Assert.assertEquals(errorCode, instance.getErrorCode());

    instance = new BusinessException(errorCode, "test error text.");
    Assert.assertEquals(errorCode, instance.getErrorCode());
  }

}
