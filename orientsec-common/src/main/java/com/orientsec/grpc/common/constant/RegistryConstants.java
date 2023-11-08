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
package com.orientsec.grpc.common.constant;

/**
 * 注册中心相关的常量
 *
 * @author sxp
 * @since V1.0 2017/3/28
 */
public class RegistryConstants {
  /**
   * 协议:grpc
   */
  public static final String GRPC_PROTOCOL = "grpc";

  /**
   * 覆盖
   */
  public static final String OVERRIDE_PROTOCOL = "override";

  /**
   * 一个服务消费者
   */
  public static final String CONSUMER_PROTOCOL = "consumer";

  /**
   * 分类
   */
  public static final String CATEGORY_KEY = "category";

  /**
   * 服务提供者集合
   */
  public static final String PROVIDERS_CATEGORY = "providers";

  /**
   * 服务消费者集合
   */
  public static final String CONSUMERS_CATEGORY = "consumers";

  /**
   * 路由规则集合
   */
  public static final String ROUTERS_CATEGORY = "routers";

  /**
   * 一条路由规则
   */
  public static final String ROUTER_PROTOCOL = "route";

  /**
   * 一条路由规则
   */
  public static final String PARAMETER_ROUTER_PROTOCOL = "param";

  /**
   * 配置信息集合
   */
  public static final String CONFIGURATORS_CATEGORY = "configurators";

  /**
   * 属于服务提供者
   */
  public static final String PROVIDER_SIDE = "provider";

  /**
   * 属于服务消费者
   */
  public static final String CONSUMER_SIDE = "consumer";

  /**
   * 动态
   */
  public static final String DYNAMIC_KEY = "dynamic";

  /**
   * 是否启用
   */
  public static final String ENABLED_KEY = "enabled";

  /**
   * 匹配任何主机IP
   */
  public static final String ANYHOST_VALUE = "0.0.0.0";

  /**
   * 匹配任何属性
   */
  public static final String ANY_VALUE = "*";

  public static final String CLIENT_REGISTRY_THREAD_NAME = "client-registry-handler";

  public static final String SERVER_REGISTRY_THREAD_NAME = "server-registry-handler";

  public static final String CONFIGURATION_REGISTRY_THREAD_NAME = "configuration-registry-handler";

}
