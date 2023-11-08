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

import java.util.Random;
import java.util.UUID;

/**
 * 通用唯一识别码生成器
 *
 * @author sxp
 * @since 2018/8/1
 */
public class UUIDGenerator {
  // 多线程场景下性能高一些
  private static final ThreadLocal<Random> tlRandom = new ThreadLocal<Random>() {
    @Override
    protected Random initialValue() {
      return new Random();
    }
  };

  /**
   * JDK提供的默认随机UUID
   * <p>
   * 长度：36
   * 底层是同步的。
   * </p>
   */
  public static String jdkRandomUUID() {
    return String.valueOf(UUID.randomUUID());
  }

  /**
   * 新的UUID生成算法
   * <p>
   * 长度：36
   * 算法来源
   * https://stackoverflow.com/questions/14532976/performance-of-random-uuid-generation-with-java-7-or-java-6
   * https://codereview.stackexchange.com/questions/19860/improving-the-java-uuid-class-performance
   * </p>
   */
  public static String newRandomUUID() {
    Random random = tlRandom.get();
    return NessUUID.toString(new UUID(random.nextLong(), random.nextLong()));
  }

}
