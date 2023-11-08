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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 监听服务提供者group参数的变化
 * <p>
 * 对服务提供者group参数的变化进行处理。
 * group参数指定当前服务器实例属于某个分组
 * </p>
 *
 * @author yulei
 * @since 2019/6/25
 */
public class ProviderGroupHandler {
  private static final Logger logger = LoggerFactory.getLogger(ProviderGroupHandler.class);
  private static final String KEY = GlobalConstants.Provider.Key.GROUP;
  private ZookeeperNameResolver zkNameResolver;

  /**
   * 记录哪些服务端参数值更新过
   * <p>
   * key值为：ip + ":" + port
   * </p>
   */
  private Map<String, Boolean> providerUpdatedMap = new HashMap<>();

  public ProviderGroupHandler(ZookeeperNameResolver zkNameResolver) {
    this.zkNameResolver = zkNameResolver;
  }


  public void notify(List<URL> urls) {
    String serviceName = zkNameResolver.getServiceName();
    Objects.requireNonNull(serviceName);
    Map<String, ServiceProvider> allProviders = zkNameResolver.getAllProviders();

    Set<String> providerIdSet = new HashSet<>(MapUtils.capacity(allProviders.size()));
    Set<Integer> providerPortSet = new HashSet<>(MapUtils.capacity(allProviders.size()));

    String providerIp, groupInitValue, id;
    int port;
    ServiceProvider provider;
    boolean isUpdate = false;
    boolean isRestore;

    //设置初始值
    for (Map.Entry<String, ServiceProvider> entry : allProviders.entrySet()) {
      provider = entry.getValue();
      providerIp = provider.getHost();
      port = provider.getPort();

      id = providerIp + ":" + port;
      providerIdSet.add(id);

      providerPortSet.add(port);

      Object groupObj = ProvidersConfigUtils.getInitProperty(serviceName, providerIp, port, KEY);
      if (groupObj == null) {
        groupInitValue = (provider.getGroup() == null ? "" : provider.getGroup());
        logger.debug("group init value is:" + groupInitValue);
        ProvidersConfigUtils.updateInitProperty(serviceName, providerIp, port, KEY, groupInitValue);
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

    String ip, providerKey, providerGroup, oldGroup;
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
            providerGroup = url.getParameter(KEY);
            if (providerGroup == null) {
              providerGroup = "";
            }
            providerGroup = providerGroup.trim();

            oldGroup = provider.getGroup();
            if (oldGroup == null) {
              oldGroup = "";
            }

            if (!providerGroup.equals(oldGroup)) {
              isUpdate = true;
              provider.setGroup(providerGroup);
              ProvidersConfigUtils.updateProperty(serviceName, providerIp, port, KEY, providerGroup);
              providerKey = providerIp + ":" + port;
              providerUpdatedMap.put(providerKey, true);

              logger.info("监听到[" + serviceName + "]服务实例[" + providerKey
                      + "]的[group]配置项，参数值为[" + providerGroup + "]");
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

    // 假设原来指定了三个服务端的[group]，这次删除了一个服务端的group配置
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

    if (isUpdate) {
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

  /**
   * 恢复参数默认值
   *
   * @return 不需要恢复数据时返回false，数据恢复成功返回true
   */
  private boolean restoreDefaultValue(String serviceName, ServiceProvider provider) {
    String providerIp = provider.getHost();
    int port = provider.getPort();

    String providerKey = providerIp + ":" + port;

    if (!providerUpdatedMap.containsKey(providerKey)
            || providerUpdatedMap.get(providerKey).booleanValue() == false) {
      return false;
    }

    Object groupObj = ProvidersConfigUtils.getInitProperty(serviceName, providerIp, port, KEY);
    String group = (groupObj == null) ? "" : groupObj.toString();

    provider.setGroup(group);
    ProvidersConfigUtils.updateProperty(serviceName, providerIp, port, KEY, group);

    providerUpdatedMap.put(providerKey, false);

    logger.info("将[" + serviceName + "]服务实例[" + providerKey
            + "]的[group]参数值恢复为默认值[" + group + "]");

    return true;
  }

}
