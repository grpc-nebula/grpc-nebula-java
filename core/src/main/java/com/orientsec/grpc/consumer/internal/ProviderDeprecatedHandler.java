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
import com.orientsec.grpc.common.util.Objects;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.common.util.TypeUtils;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 监听服务端是否过时
 *
 * @author sxp
 * @since 2019/1/30
 */
public class ProviderDeprecatedHandler {
  private static final Logger logger = LoggerFactory.getLogger(ProviderDeprecatedHandler.class);

  // 服务是否过时
  private static final String KEY = GlobalConstants.Provider.Key.DEPRECATED;

  private ZookeeperNameResolver zkNameResolver;

  /**
   * 记录哪些服务端参数值更新过
   * <p>
   * key值为：ip + ":" + port
   * </p>
   */
  private Map<String, Boolean> providerUpdatedMap = new HashMap<>();

  public ProviderDeprecatedHandler(ZookeeperNameResolver zkNameResolver) {
    this.zkNameResolver = zkNameResolver;
  }

  public void notify(List<URL> urls) {
    String serviceName = zkNameResolver.getServiceName();
    Objects.requireNonNull(serviceName);

    Map<String, ServiceProvider> allProviders = zkNameResolver.getAllProviders();

    Set<String> providerIdSet = new HashSet<>(MapUtils.capacity(allProviders.size()));
    Set<Integer> providerPortSet = new HashSet<>(MapUtils.capacity(allProviders.size()));

    String providerIp, id;
    int port;
    boolean deprecated;
    Object deprecatedObj;
    ServiceProvider provider;

    // 设置初始值
    for (Map.Entry<String, ServiceProvider> entry : allProviders.entrySet()) {
      provider = entry.getValue();
      providerIp = provider.getHost();
      port = provider.getPort();

      id = providerIp + ":" + port;
      providerIdSet.add(id);

      providerPortSet.add(port);

      deprecatedObj = ProvidersConfigUtils.getInitProperty(serviceName, providerIp, port, KEY);
      if (deprecatedObj == null) {
        deprecated = provider.isDeprecated();
        ProvidersConfigUtils.updateInitProperty(serviceName, providerIp, port, KEY, deprecated);
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

    String ip, deprecatedStr, providerKey;
    boolean newDeprecated;
    boolean hasAnyHost = false;
    int urlPort;

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
            deprecatedStr = url.getParameter(KEY);
            if (StringUtils.isEmpty(deprecatedStr)) {
              continue;
            }

            newDeprecated = TypeUtils.castToBooleanValue(deprecatedStr);

            if (newDeprecated != provider.isDeprecated()) {
              provider.setDeprecated(newDeprecated);
              ProvidersConfigUtils.updateProperty(serviceName, providerIp, port, KEY, newDeprecated);

              providerKey = providerIp + ":" + port;
              providerUpdatedMap.put(providerKey, true);

              if (newDeprecated) {
                logger.warn("监听到[" + serviceName + "]服务实例[" + providerKey
                        + "]的[是否过时]配置项，参数值为[" + newDeprecated + "]");
              } else {
                logger.info("监听到[" + serviceName + "]服务实例[" + providerKey
                        + "]的[是否过时]配置项，参数值为[" + newDeprecated + "]");
              }
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

    // 假设原来指定了三个服务端的[是否过时]，这次删除了一个服务端的是否过时配置，需要把被删除的服务端是否过时恢复为默认值
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

    Object deprecatedObj = ProvidersConfigUtils.getInitProperty(serviceName, providerIp, port, KEY);
    boolean initDeprecated = TypeUtils.castToBooleanValue(deprecatedObj);

    if (initDeprecated != provider.isDeprecated()) {
      provider.setDeprecated(initDeprecated);
      ProvidersConfigUtils.updateProperty(serviceName, providerIp, port, KEY, initDeprecated);

      providerUpdatedMap.put(providerKey, false);

      logger.info("将[" + serviceName + "]服务实例[" + providerKey
              + "]的[是否过时]参数值恢复为默认值[" + initDeprecated + "]");
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
