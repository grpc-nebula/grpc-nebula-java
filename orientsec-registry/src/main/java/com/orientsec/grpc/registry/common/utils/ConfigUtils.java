/*
 * Copyright 2018-2019 The Apache Software Foundation
 * Modifications 2019 Orient Securities Co., Ltd.
 * Modifications 2019 BoCloud Inc.
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



import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;


/**
 * 配置文件工具类
 *
 * @author heiden
 * @since 2018/3/15.
 */
public class ConfigUtils {
  //private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);

  /**
   * 配置文件的名称是否非空
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean isNotEmpty(String value) {
    return !isEmpty(value);
  }

  /**
   * 配置文件的名称是否为空
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean isEmpty(String value) {
    return value == null || value.length() == 0
            || "false".equalsIgnoreCase(value)
            || "0".equalsIgnoreCase(value)
            || "null".equalsIgnoreCase(value)
            || "N/A".equalsIgnoreCase(value);
  }

  private static int PID = -1;

  /**
   * 获取进程ID
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static int getPid() {
    if (PID < 0) {
      try {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName(); // format: "pid@hostname"
        PID = Integer.parseInt(name.substring(0, name.indexOf('@')));
      } catch (Throwable e) {
        PID = 0;
      }
    }
    return PID;
  }

  private ConfigUtils() {
  }

}
