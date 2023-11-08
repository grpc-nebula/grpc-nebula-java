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
package com.orientsec.grpc.consumer.task;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.exception.BusinessException;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.consumer.common.ConsumerConstants;
import com.orientsec.grpc.consumer.core.ConsumerConfigUtils;
import com.orientsec.grpc.consumer.core.DefaultConsumerServiceRegistryImpl;
import com.orientsec.grpc.consumer.watch.ConsumerListener;
import com.orientsec.grpc.registry.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 注册服务
 * <p>
 * 消费者调用服务时，基于配置文件，自动注册该consumer。 <br>
 * 从配置文件中获取服务注册必选参数和部分可选参数，为注册服务做准备。
 * 同时需要订阅相关的目录
 * </p>
 *
 * @author dengjq
 * @since V1.0 2017/3/30
 */
public class RegistryTask extends AbstractTask {
  private static final Logger logger = LoggerFactory.getLogger(RegistryTask.class);

  Map<String, Object> listeners;

  /**
   * consumer 运行时属性参数
   */
  private Map<String, Object> consumersParams = null;

  /**
   * 该服务器上的所有服务接口名
   */
  private List<String> interfaceNames;

  /**
   * 由consumer运行时属性参数和配置文件属性参数生成的最终属性参数
   */
  private Map<String, Object> confItem;

  public RegistryTask(DefaultConsumerServiceRegistryImpl caller, Map<String, Object> consumersParams, Map<String, Object> listeners) {
    super(caller);
    this.listeners = listeners;
    this.consumersParams = consumersParams;
  }

  /**
   * 主方法
   *
   * @author djq
   * @since V1.0 2017/3/30
   */
  public String work() throws Exception {
    if (consumersParams == null) {
      throw new BusinessException("注册服务失败:传入的参数servicesParams为空");
    }
    // 校验和保存配置文件信息
    confItem = ConsumerConfigUtils.mergeConsumerConfig(consumersParams);

    // 注册并增加监听器
    return doRegister();
  }

  private Map<String, String> initConsumerParams() {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(GlobalConstants.Consumer.Key.CATEGORY, RegistryConstants.CONSUMERS_CATEGORY);
    parameters.put(GlobalConstants.Consumer.Key.SIDE, RegistryConstants.CONSUMER_SIDE);
    parameters.put(RegistryConstants.DYNAMIC_KEY, "true");
    return parameters;
  }

  /**
   * 注册并增加监听器
   *
   * @author dengjq
   * @since V1.0 2017/3/24
   */
  private String doRegister() throws Exception {
    Map<String, Object> listenersInfo = caller.getListenersInfo();
    URL urlOfConsumer, urlOfProviders, urlOfRouters, urlOfConfigurators;
    Map<String, String> consumerInfo;
    Map<String, String> parameters;
    Object value;
    String valueOfS;
    int port = 0;

    String interfaceName = confItem.get(GlobalConstants.Consumer.Key.INTERFACE).toString();
    if (confItem.containsKey(GlobalConstants.CONSUMER_REQUEST_PORT)) {
      port = Integer.valueOf(confItem.get(GlobalConstants.CONSUMER_REQUEST_PORT).toString());
    }

    consumerInfo = initConsumerParams();
    
    //遍历配置项
    for (Map.Entry<String, Object> entry : confItem.entrySet()) {
      value = entry.getValue();
      valueOfS = (value == null) ? (null) : String.valueOf(value);
      if (!StringUtils.isEmpty(valueOfS)) {
        consumerInfo.put(entry.getKey(), valueOfS);
      }
    }

    // 注册服务(providers)
    parameters = new HashMap<String, String>(consumerInfo);
    urlOfConsumer = new URL(RegistryConstants.CONSUMER_PROTOCOL, caller.getIp(), port, interfaceName, parameters);

    logger.info( "客户端注册：" + urlOfConsumer);
    safeRegistry(urlOfConsumer);
    String uuid = urlOfConsumer.toFullString();

    listeners.put("urlOfConsumer", urlOfConsumer);

    if (listenersInfo != null && listenersInfo.size() > 0) {
      ConsumerListener providerListener = (ConsumerListener) listenersInfo.get(ConsumerConstants.PROVIDERS_LISTENER_KEY);
      if (providerListener != null) {
        logger.info( "消费者订阅providers目录"); //调低日志级别，默认不输出日志
        consumerInfo.put(RegistryConstants.CATEGORY_KEY, RegistryConstants.PROVIDERS_CATEGORY);
        parameters = new HashMap<String, String>(consumerInfo);
        urlOfProviders = new URL(RegistryConstants.GRPC_PROTOCOL, caller.getIp(), port, interfaceName, parameters);
        safeSubscribe(urlOfProviders, providerListener);

        listeners.put("urlOfProviders", urlOfProviders);
      }

      ConsumerListener routerListener = (ConsumerListener) listenersInfo.get(ConsumerConstants.ROUTERS_LISTENER_KEY);
      if (routerListener != null) {
        logger.info("消费者订阅routers目录");
        consumerInfo.put(RegistryConstants.CATEGORY_KEY, RegistryConstants.ROUTERS_CATEGORY);
        parameters = new HashMap<String, String>(consumerInfo);
        urlOfRouters = new URL(RegistryConstants.ROUTER_PROTOCOL, caller.getIp(), port, interfaceName, parameters);
        safeSubscribe(urlOfRouters, routerListener);

        listeners.put("urlOfRouters", urlOfRouters);
      }

      ConsumerListener configuratorListener = (ConsumerListener) listenersInfo.get(ConsumerConstants.CONFIGURATORS_LISTENER_KEY);
      if (configuratorListener != null) {
        logger.info("消费者订阅configurators目录");
        consumerInfo.put(RegistryConstants.CATEGORY_KEY, RegistryConstants.CONFIGURATORS_CATEGORY);
        parameters = new HashMap<String, String>(consumerInfo);
        urlOfConfigurators = new URL(RegistryConstants.OVERRIDE_PROTOCOL, caller.getIp(), port, interfaceName, parameters);
        safeSubscribe(urlOfConfigurators, configuratorListener);

        listeners.put("urlOfConfigurators", urlOfConfigurators);
      }
    }
    return uuid;
  }

}
