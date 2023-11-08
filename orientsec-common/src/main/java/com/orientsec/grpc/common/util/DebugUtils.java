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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 调试工具类
 *
 * @author sxp
 * @since V1.0 2017/5/4
 */
public final class DebugUtils {
  /**
   * 调试开关是否打开，如果没有打开该工具类的所有方法什么事情也不做
   */
  public static final boolean DEBUG = SystemSwitch.DEBUG_ENABLED;
  private static final String LOG_PREFIX = "ORIENTSEC-GRPC-DEBUG-INFO:";

  // 打印调试日志的功能列表的开关
  private static Logger logger = LoggerFactory.getLogger(DebugUtils.class);


  /**
   * 记录某个功能的运行时间
   * <p>
   * 第一次做性能测试时，发现dfzq-grpc比原生的grpc服务调用时间慢了50%。
   * 创建该方法，记录功能的运行时间，从而找出系统中耗时比较长的功能。
   * </p>
   * <p>
   * 如果调试开关未打开，不记录；如果消耗时间不是正数，不记录。
   * <p/>
   *
   * @param beginMillis  开始进行该功能时的时间戳，可以通过{@link System#currentTimeMillis()}获得
   * @param functionDesc 功能描述
   * @author sxp
   * @since V1.0 2017/5/4
   */
  public static void logElapsedTime(long beginMillis, String functionDesc) {
    if (!DEBUG) {
      return;
    }

    long end = System.currentTimeMillis();
    long elapsed = end - beginMillis;
    if (elapsed <= 0) {
      return;
    }

    String message = LOG_PREFIX + "功能[" + functionDesc + "]消耗的时间为[" + elapsed + "]毫秒";
    logger.info(message);
  }

  /**
   * 记录某个功能的运行时间
   * <p>
   * 如果调试开关未打开，不记录；如果消耗时间不是正数，不记录。
   * <p/>
   *
   * @param beginNanos   开始进行该功能时的时间戳，可以通过{@link System#nanoTime()}获得
   * @param functionDesc 功能描述
   * @author sxp
   * @since V1.0 2017/5/9
   */
  public static void logElapsedNanotime(long beginNanos, String functionDesc) {
    if (!DEBUG) {
      return;
    }

    long end = System.nanoTime();
    double elapsed = (double) (end - beginNanos) / 1000000;
    if (elapsed <= 0) {
      return;
    }

    String message = LOG_PREFIX + "功能[" + functionDesc + "]消耗的时间为[" + elapsed + "]毫秒";
    logger.info(message);
  }

}
