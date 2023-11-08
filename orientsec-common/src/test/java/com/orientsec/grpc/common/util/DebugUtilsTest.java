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

import com.orientsec.grpc.common.resource.SystemSwitch;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.TimeUnit;

/**
 * Test for DebugUtils
 *
 * @author sxp
 * @since 2018/11/28
 */
public class DebugUtilsTest {
  private static Logger logger = LoggerFactory.getLogger(DebugUtilsTest.class);
  private static String oldUserDir = System.getProperty("user.dir");
  private static boolean oldDebugFlag;

  @BeforeClass
  public static void setUp() {
    System.setProperty("user.dir", oldUserDir + "/..");
    oldDebugFlag = SystemSwitch.DEBUG_ENABLED;

    setStaticValue(SystemSwitch.class, "DEBUG_ENABLED", true);
    DebugUtils.logElapsedNanotime(System.nanoTime(), "test");
  }

  @AfterClass
  public static void tearDown() {
    System.setProperty("user.dir", oldUserDir);
    setStaticValue(SystemSwitch.class, "DEBUG_ENABLED", oldDebugFlag);
  }

  @Test
  public void useLogElapsedTime() throws Exception {
    long begin = System.currentTimeMillis();

    TimeUnit.MILLISECONDS.sleep(50);

    DebugUtils.logElapsedTime(begin, "com.orientsec.grpc.common.util.DebugUtilsTest.logElapsedTime");
  }

  @Test
  public void useLogElapsedNanotime() throws Exception {
    long begin = System.nanoTime();

    TimeUnit.NANOSECONDS.sleep(50);

    DebugUtils.logElapsedNanotime(begin, "com.orientsec.grpc.common.util.DebugUtilsTest.logElapsedNanotime");
  }


  private static <T> boolean setStaticValue(Class<?> clazz, String filedName, T newValue) {
    try {
      Field field = clazz.getField(filedName);
      field.setAccessible(true);

      /* 去除final修饰符的影响，将字段设为可修改的 */
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

      field.set(null, newValue);

      return true;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return false;
    }

  }

}
