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
import com.orientsec.grpc.common.model.BasicProvider;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务提供者配置信息工具类
 *
 * @author sxp
 * @since 2018/8/11
 */
public class ProvidersConfigUtils {
  private static Logger logger = LoggerFactory.getLogger(ProvidersConfigUtils.class);

  /**
   * 服务提供者的配置信息
   * <p>
   * 外层Map的key值为服务的接口名称  <br>
   * 内层Map的key值为服务提供者的IP:port  <br>
   * </p>
   */
  private static ConcurrentHashMap<String, ConcurrentHashMap<String, BasicProvider>> serviceProvidersConfig
          = new ConcurrentHashMap<String, ConcurrentHashMap<String, BasicProvider>>();

  /**
   * 更新服务提供者的某个属性
   *
   * @author sxp
   * @since 2018/8/11
   */
  public static void updateProperty(String serviceName, String ip, int port,
                                    String propertyKey, Object propertyValue) {
    ConcurrentHashMap<String, BasicProvider> providersConfig;

    if (serviceProvidersConfig.containsKey(serviceName)) {
      providersConfig = serviceProvidersConfig.get(serviceName);
    } else {
      providersConfig = new ConcurrentHashMap<String, BasicProvider>();

      ConcurrentHashMap<String, BasicProvider> oldValue;
      oldValue = serviceProvidersConfig.putIfAbsent(serviceName, providersConfig);
      if (oldValue != null) {
        providersConfig = oldValue;
      }
    }

    String key = ip + ":" + port;
    BasicProvider provider;

    if (providersConfig.containsKey(key)) {
      provider = providersConfig.get(key);
    } else {
      provider = createNewProvider(serviceName, ip, port, propertyKey, propertyValue);

      BasicProvider oldValue = providersConfig.putIfAbsent(key, provider);
      if (oldValue != null) {
        provider = oldValue;
      }
    }

    ConcurrentHashMap<String, Object> properties = provider.getProperties();
    properties.put(propertyKey, propertyValue);
  }

  /**
   * 更新服务提供者的某个属性的初始值
   *
   * @author sxp
   * @since 2019/1/30
   */
  public static void updateInitProperty(String serviceName, String ip, int port,
                                    String propertyKey, Object propertyValue) {
    ConcurrentHashMap<String, BasicProvider> providersConfig;

    if (serviceProvidersConfig.containsKey(serviceName)) {
      providersConfig = serviceProvidersConfig.get(serviceName);
    } else {
      providersConfig = new ConcurrentHashMap<String, BasicProvider>();

      ConcurrentHashMap<String, BasicProvider> oldValue;
      oldValue = serviceProvidersConfig.putIfAbsent(serviceName, providersConfig);
      if (oldValue != null) {
        providersConfig = oldValue;
      }
    }

    String key = ip + ":" + port;
    BasicProvider provider;

    if (providersConfig.containsKey(key)) {
      provider = providersConfig.get(key);
    } else {
      provider = createNewProvider(serviceName, ip, port, propertyKey, propertyValue);

      BasicProvider oldValue = providersConfig.putIfAbsent(key, provider);
      if (oldValue != null) {
        provider = oldValue;
      }
    }

    ConcurrentHashMap<String, Object> initProperties = provider.getInitProperties();
    initProperties.put(propertyKey, propertyValue);
  }

  /**
   * 获取服务提供者的某个属性值
   *
   * @author sxp
   * @since 2018/8/11
   */
  public static Object getProperty(String serviceName, String ip, int port, String propertyKey) {
    if (!serviceProvidersConfig.containsKey(serviceName)) {
      return null;
    }

    ConcurrentHashMap<String, BasicProvider> providersConfig = serviceProvidersConfig.get(serviceName);
    String key = ip + ":" + port;

    if (!providersConfig.containsKey(key)) {
      return null;
    }

    BasicProvider provider = providersConfig.get(key);
    ConcurrentHashMap<String, Object> properties = provider.getProperties();

    return properties.get(propertyKey);
  }

  /**
   * 获取服务提供者的某个属性的初始化值
   *
   * @author sxp
   * @since 2018/8/11
   */
  public static Object getInitProperty(String serviceName, String ip, int port, String propertyKey) {
    if (!serviceProvidersConfig.containsKey(serviceName)) {
      return null;
    }

    ConcurrentHashMap<String, BasicProvider> providersConfig = serviceProvidersConfig.get(serviceName);
    String key = ip + ":" + port;

    if (!providersConfig.containsKey(key)) {
      return null;
    }

    BasicProvider provider = providersConfig.get(key);
    ConcurrentHashMap<String, Object> initProperties = provider.getInitProperties();

    return initProperties.get(propertyKey);
  }

  private static BasicProvider createNewProvider(String serviceName, String ip, int port,
                                                 String propertyKey, Object propertyValue) {
    BasicProvider provider = new BasicProvider(serviceName, ip, port);

    ConcurrentHashMap<String, Object> properties = provider.getProperties();
    properties.put(propertyKey, propertyValue);

    return provider;
  }

  /**
   * 更新ServiceProvider对象中部分属性值
   *
   * @author sxp
   * @since 2018/8/11
   */
  public static void resetServiceProviderProperties(ServiceProvider serviceProvider) {
    String serviceName = serviceProvider.getInterfaceName();
    String ip = serviceProvider.getHost();
    int port = serviceProvider.getPort();

    // 目前客户端监听的服务端属性有：weight,deprecated,master,group
    Object value;

    value = getProperty(serviceName, ip, port, GlobalConstants.Provider.Key.WEIGHT);
    if (value != null) {
      int weight = ((Integer) value).intValue();
      serviceProvider.setWeight(weight);
    }

    value = getProperty(serviceName, ip, port, GlobalConstants.Provider.Key.DEPRECATED);
    if (value != null) {
      boolean deprecated = ((Boolean) value).booleanValue();
      serviceProvider.setDeprecated(deprecated);
    }

    value = getProperty(serviceName, ip, port, GlobalConstants.Provider.Key.MASTER);
    if (value != null) {
      boolean master = ((Boolean) value).booleanValue();
      serviceProvider.setMaster(master);
    }

    value = getProperty(serviceName, ip, port, GlobalConstants.Provider.Key.GROUP);
    if (value != null) {
      String group = (String) value;
      serviceProvider.setGroup(group);
    }
  }

  /**
   * 删除服务提供者
   * <p>
   * 客户端监控某个服务端下线后，调用该方法
   * </p>
   *
   * @param removeHostPorts 集合内元素的值为host:port，例如192.168.20.110:50051
   * @author sxp
   * @since 2019/11/20
   */
  public static void removeProperty(String serviceName, Set<String> removeHostPorts) {
    if (!serviceProvidersConfig.containsKey(serviceName)) {
      return;
    }

    ConcurrentHashMap<String, BasicProvider> providersConfig = serviceProvidersConfig.get(serviceName);
    for (String hostPort : removeHostPorts) {
      providersConfig.remove(hostPort);
    }
  }

}
