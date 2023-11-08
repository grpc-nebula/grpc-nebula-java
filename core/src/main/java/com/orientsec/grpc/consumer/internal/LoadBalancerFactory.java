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

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.consumer.lb.ConsistentHashLoadBalancer;
import com.orientsec.grpc.consumer.lb.PickFirstLoadBalancer;
import com.orientsec.grpc.consumer.lb.RoundRobinLoadBalancer;
import com.orientsec.grpc.consumer.lb.WeightRoundRobinLoadBalancer;
import com.orientsec.grpc.consumer.model.ServiceProvider;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 根据配置文件的负载策略选取算法.
 *
 * @author yangzr.
 * @since 2018-04-11 16:16.
 */
public class LoadBalancerFactory {
  private static final Logger logger = Logger.getLogger(LoadBalancerFactory.class.getName());

  public static Map<String, ServiceProvider> getServiceProviderByLbStrategy(GlobalConstants.LB_STRATEGY strategy, Map<String, ServiceProvider> serviceProviderMap, String serviceName, Object argument) {
    Map<String, ServiceProvider> serviceProviders;

    switch (strategy) {
      case PICK_FIRST:
        serviceProviders = PickFirstLoadBalancer.chooseProvider(serviceProviderMap);
        break;
      case ROUND_ROBIN:
        serviceProviders = RoundRobinLoadBalancer.chooseProvider(serviceProviderMap);
        break;
      case WEIGHT_ROUND_ROBIN:
        serviceProviders = WeightRoundRobinLoadBalancer.chooseProvider(serviceProviderMap);
        break;
      case CONSISTENT_HASH:
        serviceProviders = ConsistentHashLoadBalancer.chooseProvider(serviceProviderMap, serviceName, argument);
        break;
      default:
        serviceProviders = RoundRobinLoadBalancer.chooseProvider(serviceProviderMap);
    }

    logger.log(Level.FINE, "选择的负载均衡策略为：" + strategy.name());

    return serviceProviders;
  }
}
