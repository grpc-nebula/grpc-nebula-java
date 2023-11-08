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
 * Test for UUIDGenerator
 *
 * @author sxp
 * @since 2018/11/28
 */
public class UUIDGeneratorTest {
  @Test
  public void randomUUID() throws Exception {
    long begin = System.nanoTime();
    for (int i = 0; i < 10000; i++) {
      UUIDGenerator.newRandomUUID();
    }
    long newTime = System.nanoTime() - begin;

    begin = System.nanoTime();
    for (int i = 0; i < 10000; i++) {
      UUIDGenerator.jdkRandomUUID();
    }
    long jdkTime = System.nanoTime() - begin;

    Assert.assertTrue(newTime < jdkTime);

    System.out.println("10000 * newRandomUUID time:" + newTime);
    System.out.println("10000 * jdkRandomUUID time:" + jdkTime);
  }


}
