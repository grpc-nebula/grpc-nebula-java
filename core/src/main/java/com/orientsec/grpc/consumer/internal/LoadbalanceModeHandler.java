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
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.enums.LoadBalanceMode;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.orientsec.grpc.common.util.LoadBalanceUtil.EMPTY_METHOD;

/**
 * 监听负载均衡模式
 *
 * @author sxp
 * @since 2019/1/30
 */
public class LoadbalanceModeHandler {

  private static final Logger logger = Logger.getLogger(LoadbalanceModeHandler.class.getName());

  // 负载均衡模式
  private static final String KEY = GlobalConstants.Consumer.Key.LOADBALANCE_MODE_FOR_LISTENER;

  private static final String METHOD = GlobalConstants.Consumer.Key.METHOD;

  private ZookeeperNameResolver zkNameResolver;

  private boolean firstNotify = true;

  public LoadbalanceModeHandler(ZookeeperNameResolver zkNameResolver) {
    this.zkNameResolver = zkNameResolver;
  }

  public void notify(List<URL> urls) {
    List<URL> filteredUrls = filterUrls(urls);

    boolean isEmpty = false;
    if (filteredUrls.isEmpty()) {
      isEmpty = true;
    } else if (filteredUrls.size() == 1 && Constants.EMPTY_PROTOCOL.equals(filteredUrls.get(0).getProtocol())) {
      isEmpty = true;
    }

    if (firstNotify) {
      firstNotify = false;
    }

    Map<String, String> oldLoadBalanceModeMap = zkNameResolver.getManagedChannel().getLoadBalanceModeMap();
    Map<String, String> newLoadBalanceModeMap = new HashMap<>();
    if (oldLoadBalanceModeMap != null && oldLoadBalanceModeMap.containsKey(EMPTY_METHOD)) {
      newLoadBalanceModeMap.put(EMPTY_METHOD, oldLoadBalanceModeMap.get(EMPTY_METHOD));
    }

    if (isEmpty) {
      zkNameResolver.getManagedChannel().setLoadBalanceModeMap(newLoadBalanceModeMap);
      return;
    }

    for (URL url : filteredUrls) {
      String method = url.getParameter(METHOD);
      String newMode = url.getParameter(KEY);
      if (StringUtils.isNotEmpty(newMode)) {
        newMode = newMode.trim();
        if (!newMode.equals(LoadBalanceMode.connection.name())
                && !newMode.equals(LoadBalanceMode.request.name())) {
          newMode = null;
        }
      }
      if (StringUtils.isEmpty(newMode)) {
        continue;
      }

      if (StringUtils.isEmpty(method)) {     //服务负载均衡模式
        newLoadBalanceModeMap.put(EMPTY_METHOD, newMode);
      } else {                  //方法负载均衡模式
        newLoadBalanceModeMap.put(method, newMode);
      }
    }

    zkNameResolver.getManagedChannel().setLoadBalanceModeMap(newLoadBalanceModeMap);
  }

  private List<URL> filterUrls(List<URL> urls) {
    List<URL> filteredUrls = new ArrayList<>(urls.size());
    String consumerIP = zkNameResolver.getConsumerIP();

    String protocol, urlIp;

    int urlPort;

    for (URL url : urls) {
      if (url == null) {
        continue;
      }

      if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
        // 对于节点删除的情况也要进行处理
      } else {
        // 目前只对override操作做监听
        protocol = url.getProtocol();
        if (!RegistryConstants.OVERRIDE_PROTOCOL.equals(protocol)) {
          continue;
        }

        // 检验IP地址
        urlIp = url.getIp();
        if (StringUtils.isEmpty(urlIp)) {
          continue;
        }
        if (!RegistryConstants.ANYHOST_VALUE.equals(urlIp) && !consumerIP.equals(urlIp)) {
          continue;
        }

        // 校验端口号
        urlPort = url.getPort();
        if (urlPort != 0) {
          continue;
        }

        // 是否为当前类处理的参数值
        if (!url.getParameters().containsKey(KEY)) {
          continue;
        }
      }

      filteredUrls.add(url);
    }

    return filteredUrls;
  }
}
