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
 * 日期工具类
 *
 * @author sxp
 * @since 2018/5/24
 */
public class DateUtils {
  /**
   * 一天的毫秒总数
   */
  public static final long DAY_IN_MILLIS = 24L * 3600 * 1000;

  /**
   * 10分钟的毫秒总数
   */
  public static final long TEN_MINUTES_IN_MILLIS = TimeUnit.MINUTES.toMillis(10L);




  /**
   * Test
   */
  public static void main(String[] args) {
    System.out.println(DateUtils.TEN_MINUTES_IN_MILLIS);
  }
}
