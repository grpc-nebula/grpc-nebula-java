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
package com.orientsec.grpc.consumer.common;

/**
 * 服务消费者常量
 *
 * @author djq
 * @since V1.0 2017/3/24
 */
public class ConsumerConstants {
  /**
   * 预估的服务提供者配置属性的个数
   */
  public static final int CONFIG_CAPACITY = 32;

  /**
   * 保存订阅回调函数的key
   */

  /**
   * 服务提供者集合
   */
  public static final String PROVIDERS_LISTENER_KEY = "providers";

  /**
   * 路由规则集合
   */
  public static final String ROUTERS_LISTENER_KEY = "routers";

  /**
   * 配置集合
   */
  public static final String CONFIGURATORS_LISTENER_KEY = "configurators";
}
