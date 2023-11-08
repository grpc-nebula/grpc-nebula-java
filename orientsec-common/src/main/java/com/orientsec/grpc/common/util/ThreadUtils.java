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

import java.util.concurrent.TimeUnit;

/**
 * 线程工具类
 *
 * @author sxp
 * @since 2018/10/19
 */
public class ThreadUtils {

  /**
   * 获取当前线程的标识符
   *
   * @author sxp
   * @since 2018/10/19
   */
  public static long getCurrentId() {
    return Thread.currentThread().getId();
  }

  /**
   * 不抛异常的sleep
   *
   * @author sxp
   * @since 2019/11/13
   */
  public static void sleepQuietly(TimeUnit unit, long duration) {
    try {
      unit.sleep(duration);
    } catch (InterruptedException e) {
      //ignore
    }
  }
}
