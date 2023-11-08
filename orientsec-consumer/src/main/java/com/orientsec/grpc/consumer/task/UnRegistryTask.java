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

import com.orientsec.grpc.consumer.common.ConsumerConstants;
import com.orientsec.grpc.consumer.core.DefaultConsumerServiceRegistryImpl;
import com.orientsec.grpc.consumer.watch.ConsumerListener;
import com.orientsec.grpc.registry.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;



/**
 * 注销服务
 *
 * @author sxp
 * @since V1.0 2017/3/23
 */
public class UnRegistryTask extends AbstractTask{
  private static final Logger logger = LoggerFactory.getLogger(UnRegistryTask.class);

  Map<String, Object> listeners;
  Map<String, Object> consumersParams;

  public UnRegistryTask(DefaultConsumerServiceRegistryImpl caller, Map<String, Object> consumersParams, Map<String, Object> listeners) {
    super(caller);
    this.listeners = listeners;
    this.consumersParams = consumersParams;
  }

  /**
   * 主方法
   *
   * @author dengjq
   * @since V1.0 2017/3/31
   */
  public void work() throws Exception {
    URL urlOfConsumer, urlOfProviders, urlOfRouters, urlOfConfigurators;

    if (listeners != null && listeners.size() > 0) {
      ConsumerListener providerListener = (ConsumerListener) listeners.get(ConsumerConstants.PROVIDERS_LISTENER_KEY);
      if (providerListener != null) {
        logger.info("客户端取消订阅providers目录");
        urlOfProviders = (URL) listeners.get("urlOfProviders");
        safeUnSubscribe(urlOfProviders, providerListener);
      }

      ConsumerListener routerListener = (ConsumerListener) listeners.get(ConsumerConstants.ROUTERS_LISTENER_KEY);
      if (routerListener != null) {
        logger.info("客户端取消订阅routers目录");
        urlOfRouters = (URL) listeners.get("urlOfRouters");
        safeUnSubscribe(urlOfRouters, routerListener);
      }

      ConsumerListener configuratorListener = (ConsumerListener) listeners.get(ConsumerConstants.CONFIGURATORS_LISTENER_KEY);
      if (routerListener != null) {
        logger.info("客户端取消订阅configurators目录");
        urlOfConfigurators = (URL) listeners.get("urlOfConfigurators");
        safeUnSubscribe(urlOfConfigurators, configuratorListener);
      }

      // 注销客户端，不等待zk的定时检测
      logger.info("客户端注销注册信息");
      urlOfConsumer = (URL) listeners.get("urlOfConsumer");
      safeUnRegistry(urlOfConsumer);
    }

  }

}
