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
package com.orientsec.grpc.common;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java类私有变量操作工具类
 *
 * @author sxp
 * @since 2018/8/8
 */
public class PrivateFieldUtils {
  private static Logger logger = Logger.getLogger(PrivateFieldUtils.class.getName());

  public static <T> T getValue(Class<?> clazz, Object classInstance, String filedName) {
    T value = null;

    try {
      Field field = clazz.getDeclaredField(filedName);
      field.setAccessible(true);
      value = (T) field.get(classInstance);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }

    return value;
  }

  public static <T> boolean setValue(Class<?> clazz, Object classInstance, String filedName, T newValue) {
    try {
      Field field = clazz.getDeclaredField(filedName);
      field.setAccessible(true);
      field.set(classInstance, newValue);

      return true;
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      return false;
    }
  }
}
