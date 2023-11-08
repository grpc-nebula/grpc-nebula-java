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
 * Test for NamedThreadFactory
 *
 * @author sxp
 * @since 2018/11/29
 */
public class NamedThreadFactoryTest {

  @Test
  public void test() throws Exception {
    NamedThreadFactory factoryDefault = new NamedThreadFactory();
    Thread threadDefault = factoryDefault.newThread(new MyRunner());
    Assert.assertTrue(factoryDefault.getThreadGroup() != null);
    Assert.assertEquals("pool-1-thread-1", threadDefault.getName());

    Thread threadDefault2 = factoryDefault.newThread(new MyRunner());
    Assert.assertEquals("pool-1-thread-2", threadDefault2.getName());

    String prefix = "sxp";
    NamedThreadFactory factoryPrefix = new NamedThreadFactory(prefix);
    Thread threadPrefix = factoryPrefix.newThread(new MyRunner());
    Assert.assertEquals("sxp-thread-1", threadPrefix.getName());

    boolean daemo = true;
    prefix = "sxp-daemo";
    NamedThreadFactory factoryPrefixDaemo = new NamedThreadFactory(prefix, daemo);
    Thread threadPrefixDaemo = factoryPrefixDaemo.newThread(new MyRunner());
    Assert.assertEquals("sxp-daemo-thread-1", threadPrefixDaemo.getName());
  }


  class MyRunner implements Runnable {
    private String name;

    public void setName(String name) {
      this.name = name;
    }

    public void run() {
      System.out.println("hello " + name);
    }

  }


}
