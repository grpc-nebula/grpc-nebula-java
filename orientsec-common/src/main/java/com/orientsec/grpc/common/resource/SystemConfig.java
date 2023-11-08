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

import com.orientsec.grpc.common.OrientsecGrpcVersion;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.enums.LoadBalanceMode;
import com.orientsec.grpc.common.util.MathUtils;
import com.orientsec.grpc.common.util.PropertiesUtils;
import com.orientsec.grpc.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 系统配置信息
 *
 * @author sxp
 * @since V1.0 2017/3/27
 */
public final class SystemConfig {
  private static final Logger log = LoggerFactory.getLogger(SystemConfig.class);

  /**
   * 系统配置文件中的信息
   */
  private static Properties properties = null;

  /**
   * 系统模块开关配置信息
   */
  private static Properties switchProperties = null;

  /**
   * 负载均衡模式集合
   */
  private static Map<String, String> loadBalanceModeMap = null;

  /**
   * 服务端允许的最大连接数。
   *   启动时读取配置文件中的改值
   *   同时监控zk中configuration配置目录，如果相关参数发生变化，
   *   则把configuration目录配置的最大连接数赋值给该变量。
   */
  private static int providerMaxConnetions = GlobalConstants.Provider.DEFAULT_CONNECTIONS_NUM;


  // 只在加载该类时执行一次
  static {
    init();
    initSwitchProperties();
    initLoadBalanceModeMap();
    initMaxConnections();
  }

  /**
   * 将系统配置文件中的内容加载至内存
   *
   * @author sxp
   * @since V1.0 2017/3/27
   */
  private static void init() {
    try {
      properties = PropertiesUtils.getProperties(GlobalConstants.CONFIG_FILE_PATH);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    appendSomeProperties();
    showImportInfornation();
  }

  private static void appendSomeProperties() {
    if (properties != null) {
      properties.put(GlobalConstants.COMMON_GRPC, OrientsecGrpcVersion.VERSION);
    }
  }

  private static void initSwitchProperties() {
    try {
      switchProperties = PropertiesUtils.getProperties(GlobalConstants.SWITCH_CONFIG_FILE_PATH);
    } catch (Exception e) {
      switchProperties = new Properties();
      // log.log(Level.WARNING, e.getMessage());// 这个配置文件不是必须的
    }
  }

  /**
   * 获取系统配置文件中的信息
   *
   * @author sxp
   * @since V1.0 2017/3/27
   */
  public static Properties getProperties() {
    return properties;
  }

  /**
   * 获取系统模块开关配置信息
   *
   * @author sxp
   * @since V1.0 2017-3-30
   */
  public static Properties getSwitchProperties() {
    return switchProperties;
  }

  /**
   * 打印一些重要的信息在日志中
   *
   * @author sxp
   * @since V1.0 2017-4-24
   */
  private static void showImportInfornation() {
    if (properties == null) {
      return;
    }

    String[] keys = {GlobalConstants.COMMON_APPLICATION,
            GlobalConstants.COMMON_PROJECT,
            GlobalConstants.COMMON_OWNER,
            GlobalConstants.REGISTRY_CENTTER_ADDRESS};
    String value;
    StringBuilder sb = new StringBuilder();

    sb.append("**********程序的重要配置信息如下**********");
    sb.append(GlobalConstants.NEW_LINE);

    for (String key : keys) {
      if (properties.containsKey(key)) {
        value = properties.getProperty(key);
        if (!StringUtils.isEmpty(value)) {
          sb.append(key).append("的值为").append(value);
          sb.append(GlobalConstants.NEW_LINE);
        }
      }
    }

    sb.append("************************************************");

    log.info(sb.toString());
  }

  /**
   * 初始化负载均衡模式集合
   *
   * @Author yuanzhonglin
   * @since 2019/4/15
   * @since 2020/06/15 modify by wlh 修改默认负载均衡模式为请求负载均衡模式
   */
  private static void initLoadBalanceModeMap() {
    // 默认为请求模式
    String defaultValue = LoadBalanceMode.request.name();
    String mode = defaultValue;

    String key = GlobalConstants.Consumer.Key.LOADBALANCE_MODE;
    if (properties != null && properties.containsKey(key)) {
      mode = properties.getProperty(key);
      if (mode != null) {
        mode = mode.trim();
        if (!mode.equals(LoadBalanceMode.connection.name())
                && !mode.equals(LoadBalanceMode.request.name())) {
          mode = defaultValue;
        }
      }
    }

    loadBalanceModeMap = new HashMap<>();
    loadBalanceModeMap.put(GlobalConstants.LOAD_BALANCE_EMPTY_METHOD, mode);
  }

  public static Map<String, String> getLoadBalanceModeMap() {
    return loadBalanceModeMap;
  }

  /**
   * 初始化provider端允许的最大链接数
   */
  private static void initMaxConnections() {
    String keyString = RegistryConstants.PROVIDER_SIDE + "." + GlobalConstants.Provider.Key.DEFAULT_CONNECTION;
    if (properties != null && properties.containsKey(keyString)) {
      String value = properties.getProperty(keyString);
      if (MathUtils.isInteger(value)) {
        providerMaxConnetions = Integer.parseInt(value);
      }
    }
  }


  /**
   * 获取服务端当前允许的最大连接数
   */
  public static int getProviderMaxConnetions() {
    return providerMaxConnetions;
  }

  /**
   * 设置服务端当前允许的最大连接数
   * zk下，configuration目录下连接数配置发生变化时设置本变量
   */
  public static void setProviderMaxConnetions(int providerMaxConnetions) {
    SystemConfig.providerMaxConnetions = providerMaxConnetions;
  }


}
