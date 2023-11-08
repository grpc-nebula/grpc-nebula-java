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
import com.orientsec.grpc.common.util.MapUtils;
import com.orientsec.grpc.common.util.MathUtils;
import com.orientsec.grpc.common.util.Objects;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 监听服务端权重
 *
 * @author sxp
 * @since 2019/1/30
 */
public class ProviderWeightHandler {
  private static final Logger logger = Logger.getLogger(ProviderWeightHandler.class.getName());

  // 服务权重
  private static final String KEY = GlobalConstants.Provider.Key.WEIGHT;

  private ZookeeperNameResolver zkNameResolver;

  /**
   * 记录哪些服务端参数值更新过
   * <p>
   * key值为：ip + ":" + port
   * </p>
   */
  private Map<String, Boolean> providerUpdatedMap = new HashMap<>();

  public ProviderWeightHandler(ZookeeperNameResolver zkNameResolver) {
    this.zkNameResolver = zkNameResolver;
  }

  public void notify(List<URL> urls) {
    String serviceName = zkNameResolver.getServiceName();
    Objects.requireNonNull(serviceName);

    Map<String, ServiceProvider> allProviders = zkNameResolver.getAllProviders();

    Set<String> providerIdSet = new HashSet<>(MapUtils.capacity(allProviders.size()));
    Set<Integer> providerPortSet = new HashSet<>(MapUtils.capacity(allProviders.size()));

    String providerIp, id;
    int port, weight;
    Object weightObj;
    ServiceProvider provider;

    // 设置初始值
    for (Map.Entry<String, ServiceProvider> entry : allProviders.entrySet()) {
      provider = entry.getValue();
      providerIp = provider.getHost();
      port = provider.getPort();

      id = providerIp + ":" + port;
      providerIdSet.add(id);

      providerPortSet.add(port);

      weightObj = ProvidersConfigUtils.getInitProperty(serviceName, providerIp, port, KEY);
      if (weightObj == null) {
        weight = provider.getWeight();
        ProvidersConfigUtils.updateInitProperty(serviceName, providerIp, port, KEY, weight);
      }
    }

    List<URL> filteredUrls = filterUrls(urls, providerIdSet, providerPortSet);

    boolean isEmpty = false;
    if (filteredUrls.isEmpty()) {
      isEmpty = true;
    } else if (filteredUrls.size() == 1 && Constants.EMPTY_PROTOCOL.equals(filteredUrls.get(0).getProtocol())) {
      isEmpty = true;
    }

    Set<String> urlIdSet = new HashSet<>(MapUtils.capacity(filteredUrls.size()));

    String ip, weightStr, providerKey;
    int newWeight, urlPort;
    boolean hasAnyHost = false;

    if (!isEmpty) {
      for (URL url : filteredUrls) {
        ip = url.getIp();
        urlPort = url.getPort();
        if (RegistryConstants.ANYHOST_VALUE.equals(ip)) {
          hasAnyHost = true;
        }

        id = ip + ":" + urlPort;
        urlIdSet.add(id);

        for (Map.Entry<String, ServiceProvider> entry : allProviders.entrySet()) {
          provider = entry.getValue();
          providerIp = provider.getHost();
          port = provider.getPort();

          if (urlPort == port && (RegistryConstants.ANYHOST_VALUE.equals(ip) || ip.equals(providerIp))) {
            weightStr = url.getParameter(KEY);
            if (StringUtils.isEmpty(weightStr) || !MathUtils.isInteger(weightStr)) {
              continue;
            }

            newWeight = Integer.parseInt(weightStr);

            if (newWeight > 0 && provider.getWeight() > 0 && newWeight != provider.getWeight()) {
              provider.setWeight(newWeight);
              ProvidersConfigUtils.updateProperty(serviceName, providerIp, port, KEY, newWeight);

              providerKey = providerIp + ":" + port;
              providerUpdatedMap.put(providerKey, true);

              logger.info("监听到[" + serviceName + "]服务实例[" + providerKey
                      + "]的[服务权重]配置项，参数值为[" + newWeight + "]");
            }
          }
        }
      }
    } else {
      // 将参数值更新为默认值
      for (Map.Entry<String, ServiceProvider> entry : allProviders.entrySet()) {
        provider = entry.getValue();
        restoreDefaultValue(serviceName, provider);
      }
    }

    // 假设原来指定了三个服务端的[服务权重]，这次删除了一个服务端的服务权重配置，需要把被删除的服务端服务权重恢复为默认值
    if (!isEmpty && !hasAnyHost) {
      for (Map.Entry<String, ServiceProvider> entry : allProviders.entrySet()) {
        provider = entry.getValue();
        providerIp = provider.getHost();
        port = provider.getPort();

        id = providerIp + ":" + port;
        if (urlIdSet.contains(id)) {
          continue;
        }

        restoreDefaultValue(serviceName, provider);
      }
    }
  }

  private void restoreDefaultValue(String serviceName, ServiceProvider provider) {
    String providerIp = provider.getHost();
    int port = provider.getPort();

    String providerKey = providerIp + ":" + port;

    if (!providerUpdatedMap.containsKey(providerKey)
            || providerUpdatedMap.get(providerKey).booleanValue() == false) {
      return;
    }

    Object weightObj = ProvidersConfigUtils.getInitProperty(serviceName, providerIp, port, KEY);
    int initWeight = MathUtils.castToIntNoException(weightObj);

    if (initWeight > 0 && provider.getWeight() > 0 && initWeight != provider.getWeight()) {
      provider.setWeight(initWeight);
      ProvidersConfigUtils.updateProperty(serviceName, providerIp, port, KEY, initWeight);

      providerUpdatedMap.put(providerKey, false);

      logger.info("将[" + serviceName + "]服务实例[" + providerKey
              + "]的[服务权重]参数值恢复为默认值[" + initWeight + "]");
    }
  }

  private List<URL> filterUrls(List<URL> urls, Set<String> providerIdSet, Set<Integer> providerPortSet) {
    List<URL> filteredUrls = new ArrayList<>(urls.size());

    String protocol, urlIp, id;
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
        urlPort = url.getPort();
        id = urlIp + ":" + urlPort;

        if (StringUtils.isEmpty(urlIp)) {
          continue;
        }
        if (!RegistryConstants.ANYHOST_VALUE.equals(urlIp) && !providerIdSet.contains(id)) {
          continue;
        }
        if (RegistryConstants.ANYHOST_VALUE.equals(urlIp) && !providerPortSet.contains(urlPort)) {
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
