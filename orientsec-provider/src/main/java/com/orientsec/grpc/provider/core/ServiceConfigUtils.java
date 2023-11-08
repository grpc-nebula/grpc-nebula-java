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
package com.orientsec.grpc.provider.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务配置信息工具类
 *
 * @author sxp
 * @since V1.0 2017/3/22
 */
public class ServiceConfigUtils {
  /**
   * 所有服务提供者的当前配置信息
   * <p>
   * key值为服务的interface属性的值,value为存储的是对应服务的属性键值对
   * </p>
   */
  private static Map<String, Map<String, Object>> currentServicesConfig
          = new ConcurrentHashMap<>();

  /**
   * 所有服务提供者的初始配置信息
   * <p>
   * key值为服务的interface属性的值,value为存储的是对应服务的属性键值对
   * </p>
   */
  private static Map<String, Map<String, Object>> initialServicesConfig
          = new ConcurrentHashMap<>();

  /**
   * 获取所有服务提供者的当前配置信息
   *
   * @author sxp
   * @since V1.0 2017/3/22
   */
  public static Map<String, Map<String, Object>> getCurrentServicesConfig() {
    return currentServicesConfig;
  }

  /**
   * 获取所有服务提供者的初始配置信息
   *
   * @author sxp
   * @since 2019/1/7
   */
  public static Map<String, Map<String, Object>> getInitialServicesConfig() {
    return initialServicesConfig;
  }
}
