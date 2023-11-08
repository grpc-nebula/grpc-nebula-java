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
/*
 * Copyright 2015, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.orientsec.grpc.consumer.lb;

import com.google.common.base.Preconditions;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.consumer.strategy.Server;
import com.orientsec.grpc.consumer.strategy.WeightRoundRobin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加权轮询
 *
 * @author bona
 * @since 2018-04-13 13:43
 * @since 2018-4-20 modify by sxp 用ConcurrentHashMap来取代对象锁
 */
public class WeightRoundRobinLoadBalancer {
  /**
   * 作为全局变量存放，接口对应的服务列表
   * <p>
   * key值为接口名称
   * <p/>
   */
  private static ConcurrentHashMap<String, WeightRoundRobin> serviceMap = new ConcurrentHashMap<>();

  /**
   * 对提供的服务列表运用权重负载均衡算法，获取一条服务供调用
   *
   * @param serviceProviders 提供指定接口的服务列表
   * @return 运用算法选出的一台服务器
   */
  public static Map<String, ServiceProvider> chooseProvider(Map<String, ServiceProvider> serviceProviders) {
    if (serviceProviders == null || serviceProviders.isEmpty() || serviceProviders.size() == 1) {
      return serviceProviders;
    }
    // 获取接口名称
    String interfaceName = RoundRobinLoadBalancer.getInterfaceName(serviceProviders);
    Preconditions.checkNotNull(interfaceName, "interfaceName");

    Server server = getWeightRoundRobin(interfaceName, serviceProviders).getBestServer();
    Preconditions.checkNotNull(server, "server");

    Map<String, ServiceProvider> serverMap = new ConcurrentHashMap<String, ServiceProvider>();
    serverMap.put(server.getKey(), serviceProviders.get(server.getKey()));
    return serverMap;
  }

  /**
   * 获取接口名称，权重对象
   *
   * @param interfaceName    接口名称
   * @param serviceProviders 服务类别
   * @return 权重算法对象
   */
  private static WeightRoundRobin getWeightRoundRobin(String interfaceName, Map<String, ServiceProvider> serviceProviders) {
    WeightRoundRobin wrr;

    if (!serviceMap.containsKey(interfaceName)) {
      wrr = newWeightRoundRobin(serviceProviders);
      if (null != wrr) {
        WeightRoundRobin oldValue = serviceMap.putIfAbsent(interfaceName, wrr);
        if (oldValue != null) {
          wrr = oldValue;// 考虑多线程
        }
      }
    } else {
      wrr = serviceMap.get(interfaceName);

      if (isChanged(wrr, serviceProviders)) {
        WeightRoundRobin newWrr = newWeightRoundRobin(serviceProviders);
        if (null != newWrr) {
          serviceMap.put(interfaceName, newWrr);
        }
        wrr = newWrr;
      }
    }

    return wrr;
  }

  /**
   * 根据serviceProviders构建WeightRoundRobin对象
   *
   * @param serviceProviders 经过黑白名单筛选的服务列表
   */
  private static WeightRoundRobin newWeightRoundRobin(Map<String, ServiceProvider> serviceProviders) {
    WeightRoundRobin wrr = new WeightRoundRobin();

    // 将Map里面的value值放到一个List中
    List<Server> serverList = new ArrayList<>();

    for (Map.Entry<String, ServiceProvider> entry : serviceProviders.entrySet()) {
      Server server = new Server(entry.getKey(), entry.getValue().getWeight());
      serverList.add(server);
    }

    wrr.setServers(serverList);

    return wrr;
  }

  /**
   * 校验服务列表获取权重值是否已经发生变更.
   *
   * @param wrr              暂存的服务对象
   * @param serviceProviders 当前获取到的服务列表
   * @return true 服务列表或权重发生变化 false 服务列表或权重未发生变化
   */
  private static boolean isChanged(WeightRoundRobin wrr, Map<String, ServiceProvider> serviceProviders) {
    if (null == wrr) {
      return true;
    }

    List<Server> serverList = wrr.getServers();
    if (serverList.isEmpty()) {
      return true;
    }

    if (serverList.size() != serviceProviders.size()) {
      return true;
    }

    for (Server server : serverList) {
      if (!serviceProviders.containsKey(server.getKey())) {
        return true;
      }
      // weight值发生变更
      if (serviceProviders.get(server.getKey()).getWeight() != server.getWeight()) {
        return true;
      }
    }

    return false;
  }

}
