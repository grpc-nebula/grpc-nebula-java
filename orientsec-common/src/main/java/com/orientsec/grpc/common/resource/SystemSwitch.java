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
package com.orientsec.grpc.common.resource;


import com.orientsec.grpc.common.util.StringUtils;

import java.util.Properties;

import static com.orientsec.grpc.common.constant.GlobalConstants.Switch.Key;

/**
 * 系统开发信息
 *
 * @author sxp
 * @since V1.0 2017/3/30
 */
public class SystemSwitch {
  /**
   * 是否为调试模式
   */
  public static final boolean DEBUG_ENABLED = initSwitch(Key.DEBUG_ENABLED, false);

  /**
   * 是否启用provider的功能
   */
  public static final boolean PROVIDER_ENABLED = initSwitch(Key.PROVIDER_ENABLED, true);

  /**
   * 是否启用concumer的功能
   */
  public static final boolean CONSUMER_ENABLED = initSwitch(Key.CONSUMER_ENABLED, true);

  /**
   * 是否启用写kafka的功能
   */
  public static final boolean WRITEKAFKA_ENABLED = initSwitch(Key.WRITEKAFKA_ENABLED, true);

  /**
   * 是否打印服务链信息
   */
  public static final boolean PRINTSERVICECHAIN_ENABLED = initSwitch(Key.PRINTSERVICECHAIN_ENABLED, false);

  /**
   * 调试模式下的日志开关
   */
  public static final String LOG_SWITCH = initLogSwitch(Key.LOG_SWITCH);

  /**
   * 初始化orientsec-switch.properties文件中的属性
   *
   * @author sxp
   * @since V1.0 2017-5-4
   */
  private static boolean initSwitch(String key, boolean defaultValue) {
    boolean enabled = defaultValue;// 默认值

    Properties pros = SystemConfig.getSwitchProperties();

    if (pros.containsKey(key)) {
      String value = pros.getProperty(key);
      enabled = Boolean.valueOf(value);
    }

    return enabled;
  }

  /**
   * 调试模式下的日志开关
   */
  private static String initLogSwitch(String key) {
    Properties pros = SystemConfig.getSwitchProperties();

    String defaultValue = "11111111111111111111111111111111111111111111111111";
    int len = defaultValue.length();

    if (pros.containsKey(key)) {
      String value = pros.getProperty(key);
      if (StringUtils.isEmpty(value)) {
        return defaultValue;
      }

      value = (value + defaultValue).substring(0, len);
      return value;
    }

    return defaultValue;
  }
}
