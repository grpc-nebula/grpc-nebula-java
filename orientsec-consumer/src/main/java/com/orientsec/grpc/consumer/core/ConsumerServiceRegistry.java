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
package com.orientsec.grpc.consumer.core;

import com.orientsec.grpc.common.exception.BusinessException;
import com.orientsec.grpc.consumer.watch.ConsumerListener;
import com.orientsec.grpc.registry.common.URL;

import java.util.List;
import java.util.Map;

/**
 * 服务注册
 * <p>
 * 1.客户端请求服务前，自动注册该服务 <br>
 * 在注册时，同时需要订阅providers目录，router目录，configurator目录
 * </p>
 *
 * @author dengjq
 * @since V1.0 2017/3/20
 */
public interface ConsumerServiceRegistry {

  /**
   *
   * @param consumerParams  消费者参数map
   * @param providersListener   订阅providers回调函数
   * @param routersListener   订阅routers回调函数
   * @param configuratorsListener   订阅configurators回调函数
   * @return 本次订阅的唯一标识，主要用于退出时取消订阅
   * @throws BusinessException
   */
  String register(Map<String, Object> consumerParams,
                  ConsumerListener providersListener,
                  ConsumerListener routersListener,
                  ConsumerListener configuratorsListener) throws BusinessException;


  /**
   * 取消指定订阅，参数为空时取消最后一次订阅
   * @param subscribeId 注册时返回的订阅subscribe，可为空，为空时
   * @throws BusinessException
   */
  void unSubscribe(String subscribeId) throws BusinessException;

  /**
   * 查询provider
   * @param providerParams 查询服务时提供的查询参数
   * @throws BusinessException
   */
  List<URL> lookup(Map<String, String> providerParams) throws BusinessException;

  /**
   * 初始化注册中心连接参数
   * @param targetUrl  注册中心地址列表
   */
  ConsumerServiceRegistry forTarget(URL targetUrl);

  /**
   * 构建注册中心连接实例
   * @return ConsumerServiceRegistry
   */
  ConsumerServiceRegistry build();

}
