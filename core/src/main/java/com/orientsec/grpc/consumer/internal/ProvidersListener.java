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

import com.orientsec.grpc.common.util.MapUtils;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.consumer.watch.ConsumerListener;
import com.orientsec.grpc.registry.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端对服务提供者列表的监听
 */
public class ProvidersListener extends AbstractListener implements ConsumerListener {
  private static final Logger logger = LoggerFactory.getLogger(ProvidersListener.class);
  private boolean initData;
  private boolean isProviderListEmpty = true;
  private Set<String> previousHostPorts = new HashSet<>();

  public ProvidersListener() {
    initData = true;
  }

  @Override
  public void notify(List<URL> urls) {
    if (initData && !zookeeperNameResolver.isConnectionZkSuccess()) {
      zookeeperNameResolver.setConnectionZkSuccess(true);
      logger.info("检测到已经连接上zookeeper");
    }

    Map<String, ServiceProvider> newProviders = zookeeperNameResolver.getProvidersByUrls(urls);
    int newSize = newProviders.size();

    if (newSize == 0) {
      isProviderListEmpty = true;
    } else {
      isProviderListEmpty = false;
    }

    String serviceName = zookeeperNameResolver.getServiceName();
    logger.info("监听到{}客户端的服务器列表发生变化，当前服务端的个数为{}", serviceName, newSize);

    dealOfflineProviders(newProviders);

    Map<String, ServiceProvider> services = zookeeperNameResolver.getServiceProviderMap();

    Object lock = zookeeperNameResolver.getLock();
    synchronized (lock) {
      if (services != null) {
        services.clear();
      } else {
        services = new ConcurrentHashMap<String, ServiceProvider>();
      }
      services.putAll(newProviders);

      // 服务列表变化后，重置providersForLoadBalance
      zookeeperNameResolver.setProvidersForLoadBalance(new ConcurrentHashMap<String, ServiceProvider>());
      zookeeperNameResolver.setProvidersForLoadBalanceFlag(0);
    }

    // 为了支持zk不可用时客户端也能正常启动，这个地方判断initData的限制去掉
    zookeeperNameResolver.resolveServerInfoWithLock();

    initData = false;
  }

  /**
   * 处理离线服务端
   * <p>
   * 1. 删除当前客户端与离线服务端之间的transport缓存  <br>
   * 2. 删除缓存中该离线服务端的数据
   * </p>
   *
   * @author sxp
   * @since 2019/11/19
   */
  private void dealOfflineProviders(Map<String, ServiceProvider> newProviders) {
    Set<String> currentHostPorts = newProviders.keySet();
    Set<String> removeHostPorts = new HashSet<>(MapUtils.capacity(currentHostPorts.size()));

    if (!previousHostPorts.isEmpty()) {
      for (String hostPort : previousHostPorts) {
        if (!currentHostPorts.contains(hostPort)) {
          removeHostPorts.add(hostPort);
        }
      }
    }

    // 保存当前服务端列表
    previousHostPorts.clear();
    previousHostPorts.addAll(currentHostPorts);

    if (!removeHostPorts.isEmpty()) {
      String serviceName = zookeeperNameResolver.getServiceName();

      // 删除客户端与离线服务端之间的无效subchannel缓存
      zookeeperNameResolver.removeInvalidCacheSubchannels(removeHostPorts);

      // 删除缓存中该离线服务端的数据
      ProvidersConfigUtils.removeProperty(serviceName, removeHostPorts);
    }
  }


  public boolean isProviderListEmpty() {
    return isProviderListEmpty;
  }

  public void setProviderListEmpty(boolean providerListEmpty) {
    isProviderListEmpty = providerListEmpty;
  }
}
