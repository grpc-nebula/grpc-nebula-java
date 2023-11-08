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


package com.orientsec.grpc.consumer.internal;

import com.orientsec.grpc.consumer.watch.ConsumerListener;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.common.UrlIpComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * 客户端监听器
 *
 * @author sxp
 * @since 2019/1/30
 */
public class ConfiguratorsListener extends AbstractListener implements ConsumerListener {
  private static final Logger logger = LoggerFactory.getLogger(ConfiguratorsListener.class);

  private ProviderWeightHandler weightHandler;
  private ProviderDeprecatedHandler deprecatedHandler;
  private LoadbalanceModeHandler modeHandler;
  private LoadbalanceHandler lbHandler;
  private ServiceVersionHandler versionHandler;
  private ConsumerRequestsHandler requestsHandler;
  private ProviderMasterHandler masterHandler;
  private ProviderGroupHandler groupHandler;
  private ConsumerGroupHandler consumerGroupHandler;
  private ConsumerMasterHander consumerMasterHander;

  public ConfiguratorsListener() {
  }

  @Override
  public void notify(List<URL> urls) {
    //首先排序，确保明确的IP的规则放在最后(明确的IP，优先级高于0.0.0.0)
    Collections.sort(urls, new UrlIpComparator());

    // 监听服务端权重 weight
    if (weightHandler == null) {
      weightHandler = new ProviderWeightHandler(zookeeperNameResolver);
    }
    weightHandler.notify(urls);

    //对服务端master参数进行处理
    if(masterHandler == null){
      masterHandler = new ProviderMasterHandler(zookeeperNameResolver);
    }
    masterHandler.notify(urls);

    //对服务端group参数进行处理
    if(groupHandler == null){
      groupHandler = new ProviderGroupHandler(zookeeperNameResolver);
    }
    groupHandler.notify(urls);

    // 监听服务端是否过时 deprecated
    if (deprecatedHandler == null) {
      deprecatedHandler = new ProviderDeprecatedHandler(zookeeperNameResolver);
    }
    deprecatedHandler.notify(urls);

    // 监听负载均衡模式 loadbalance.mode
    if (modeHandler == null) {
      modeHandler = new LoadbalanceModeHandler(zookeeperNameResolver);
    }
    modeHandler.notify(urls);

    // 监听负载均衡算法 default.loadbalance
    if (lbHandler == null) {
      lbHandler = new LoadbalanceHandler(zookeeperNameResolver);
    }
    lbHandler.notify(urls);

    // 监听客户端指定的服务端版本号 service.version
    if (versionHandler == null) {
      versionHandler = new ServiceVersionHandler(zookeeperNameResolver);
    }
    versionHandler.notify(urls);

    // 监听客户端对服务端的每秒钟的请求次数参数 consumer.default.requests
    if (requestsHandler == null) {
      requestsHandler = new ConsumerRequestsHandler(zookeeperNameResolver);
    }
    requestsHandler.notify(urls);

    //监听客户端GROUP变化
    if(consumerGroupHandler == null){
      consumerGroupHandler = new ConsumerGroupHandler(zookeeperNameResolver);
    }
    consumerGroupHandler.notify(urls);

    if (consumerMasterHander == null) {
      consumerMasterHander = new ConsumerMasterHander(zookeeperNameResolver);
    }
    consumerMasterHander.notify(urls);

  }
}
