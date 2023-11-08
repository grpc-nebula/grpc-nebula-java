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
import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * 监听服务提供者master参数的变化
 * <p>
 * 对服务提供者master参数的变化进行处理。
 * master参数指定当前服务器实例是否为主服务器
 * </p>
 *
 * @author yulei
 * @since 2019/6/19
 */
public class ProviderMasterHandler {
  private static final Logger logger = LoggerFactory.getLogger(ProviderMasterHandler.class);
  // 服务master
  private static final String KEY = GlobalConstants.Provider.Key.MASTER;
  private ZookeeperNameResolver zkNameResolver;

  /**
   * 记录哪些服务端参数值更新过
   * <p>
   * key值为：ip + ":" + port
   * </p>
   */
  private Map<String, Boolean> providerUpdatedMap = new HashMap<>();

  // 记录更新后的值
  Map<String, Boolean> providerNewValueMap;

  public ProviderMasterHandler(ZookeeperNameResolver zkNameResolver) {
    this.zkNameResolver = zkNameResolver;
  }

  public void notify(List<URL> urls) {
    String serviceName = zkNameResolver.getServiceName();
    Objects.requireNonNull(serviceName);
    Map<String, ServiceProvider> allProviders = zkNameResolver.getAllProviders();

    Set<String> providerIdSet = new HashSet<>(MapUtils.capacity(allProviders.size()));
    Set<Integer> providerPortSet = new HashSet<>(MapUtils.capacity(allProviders.size()));

    providerNewValueMap = new HashMap<>();

    String providerIp, id;
    int port;
    boolean master;
    ServiceProvider provider;
    boolean isUpdate = false;
    boolean isRestore;

    // 设置初始值
    for (Map.Entry<String, ServiceProvider> entry : allProviders.entrySet()) {
      provider = entry.getValue();
      providerIp = provider.getHost();
      port = provider.getPort();

      id = providerIp + ":" + port;
      providerIdSet.add(id);

      providerPortSet.add(port);

      Object masterObj = ProvidersConfigUtils.getInitProperty(serviceName, providerIp, port, KEY);
      if (masterObj == null) {
        master = provider.getMaster();
        logger.debug("master init value is:" + master);
        ProvidersConfigUtils.updateInitProperty(serviceName, providerIp, port, KEY, master);
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

    String ip, masterStr, providerKey;
    boolean masterTemp;
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
            masterStr = url.getParameter(KEY);
            if (StringUtils.isEmpty(masterStr)) {
              masterStr = String.valueOf(GlobalConstants.Provider.DEFAULT_MASTER);
            }
            masterTemp = Boolean.valueOf(masterStr);

            if (masterTemp != provider.getMaster()) {
              isUpdate = true;

              provider.setMaster(masterTemp);
              ProvidersConfigUtils.updateProperty(serviceName, providerIp, port, KEY, masterTemp);

              providerKey = providerIp + ":" + port;
              providerUpdatedMap.put(providerKey, true);
              providerNewValueMap.put(providerKey, masterTemp);

              logger.info("监听到[" + serviceName + "]服务实例[" + providerKey
                      + "]的[master]配置项，参数值为[" + masterTemp + "]");
            }
          }
        }
      }
    } else {
      // 将参数值更新为默认值
      for (Map.Entry<String, ServiceProvider> entry : allProviders.entrySet()) {
        provider = entry.getValue();
        isRestore = restoreDefaultValue(serviceName, provider);
        if (isRestore) {
          isUpdate = true;
        }
      }
    }

    // 假设原来指定了三个服务端的[master]，这次删除了一个服务端的master配置，需要把被删除的服务端master恢复为默认值
    if (!isEmpty && !hasAnyHost) {
      for (Map.Entry<String, ServiceProvider> entry : allProviders.entrySet()) {
        provider = entry.getValue();
        providerIp = provider.getHost();
        port = provider.getPort();

        id = providerIp + ":" + port;
        if (urlIdSet.contains(id)) {
          continue;
        }
        isRestore = restoreDefaultValue(serviceName, provider);
        if (isRestore) {
          isUpdate = true;
        }
      }
    }

    // 备->主的配置变化不会引起服务提供者重选
    boolean needReselect = false;
    if (isUpdate) {
      for (Map.Entry<String, Boolean> entry : providerNewValueMap.entrySet()) {
        if (!entry.getValue()) {
          // 该provider的新master参数值为false，即 主->备
          needReselect = true;
          break;
        }
      }
    }

    if (isUpdate && needReselect) {
      // 主动触发客户端重新更新服务端列表，并重新选择服务提供者
      Object lock = zkNameResolver.getLock();
      synchronized (lock) {
        zkNameResolver.getAllByName(serviceName);
        try {
          logger.info("重选服务提供者");
          zkNameResolver.resolveServerInfo(null, null);
        } catch (Throwable t) {
          logger.error("重选服务提供者出错", t);
        }
      }
    }

  }


  /**
   * 恢复参数默认值
   *
   * @return 不需要恢复数据时返回false，数据恢复成功返回true
   */
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


  private boolean restoreDefaultValue(String serviceName, ServiceProvider provider) {
    String providerIp = provider.getHost();
    int port = provider.getPort();

    String providerKey = providerIp + ":" + port;

    if (!providerUpdatedMap.containsKey(providerKey)
            || providerUpdatedMap.get(providerKey).booleanValue() == false) {
      return false;
    }

    Object masterObj = ProvidersConfigUtils.getInitProperty(serviceName, providerIp, port, KEY);
    if (masterObj == null) {
      masterObj = String.valueOf(GlobalConstants.Provider.DEFAULT_MASTER);
    }
    boolean master = Boolean.valueOf(masterObj.toString());
    if (provider.getMaster() != master) {
      providerNewValueMap.put(providerKey, master);
    }

    provider.setMaster(master);
    ProvidersConfigUtils.updateProperty(serviceName, providerIp, port, KEY, master);

    providerUpdatedMap.put(providerKey, false);

    logger.info("将[" + serviceName + "]服务实例[" + providerKey + "]的[master]参数值恢复为默认值[" + master + "]");

    return true;
  }

}
