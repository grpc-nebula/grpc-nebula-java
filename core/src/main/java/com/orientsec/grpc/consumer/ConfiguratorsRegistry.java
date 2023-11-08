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
package com.orientsec.grpc.consumer;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.consumer.internal.ZookeeperNameResolver;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.service.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置注册工具类
 *
 * @author zhuyujie
 * @since 2020/6/3
 */
public class ConfiguratorsRegistry {

  private final Logger logger = LoggerFactory.getLogger(ConfiguratorsRegistry.class);

  private final Consumer consumer;

  private final ZookeeperNameResolver nameResolver;

  public ConfiguratorsRegistry(ZookeeperNameResolver nameResolver) {
    this.nameResolver = nameResolver;
    consumer = new Consumer(nameResolver.getZkRegistryURL());
  }

  /**
   * 保存调用主备配置项
   *
   * @author zhuyujie
   * @since 2020-6-3
   */
  public void saveInvokeMasterConfig(boolean invokeMaster) {
    String serviceName = nameResolver.getServiceName();
    String key = GlobalConstants.Consumer.Key.CONSUMER_MASTER;
    Map<String, String> parameters = initConfiguratorParameters();
    parameters.put(key, Boolean.toString(invokeMaster));
    URL url = new URL(RegistryConstants.OVERRIDE_PROTOCOL, RegistryConstants.ANYHOST_VALUE, 0, serviceName, parameters);

    configuratorSafeRegister(url, key);
    logger.info("服务[" + serviceName + "]向configurators目录注册[invoke.master]参数，参数值为[" + invokeMaster + "]");
  }

  /**
   * 初始化基础configurations类型的URL
   *
   * @author zhuyujie
   * @since 2020-6-3
   */
  private Map<String, String> initConfiguratorParameters() {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(GlobalConstants.Consumer.Key.CATEGORY, RegistryConstants.CONFIGURATORS_CATEGORY);
    parameters.put(RegistryConstants.DYNAMIC_KEY, "false");
    parameters.put(RegistryConstants.ENABLED_KEY, "true");
    return parameters;
  }

  /**
   * 向注册中心注册configurations.
   *
   * @author zhuyujie
   * @since 2020-6-3
   */
  public void configuratorSafeRegister(URL url, String key) {
    // 首先查找该key的配置项，如果值与需要注册的不同，则删除
    URL commonUrl = new URL(RegistryConstants.OVERRIDE_PROTOCOL,
        RegistryConstants.ANYHOST_VALUE, 0, nameResolver.getServiceName(), initConfiguratorParameters());

    List<URL> searchedUrls =  consumer.lookup(commonUrl);
    List<URL> filteredUrls = filterUrls(searchedUrls, url, key);
    for (URL searchedUrl : filteredUrls) {
      if (!url.getParameter(key).equals(searchedUrl.getParameter(key))) {
        configuratorUnRegister(searchedUrl);
      }
    }
    consumer.registerService(url);
  }

  /**
   * 从注册中心删除configurations.
   *
   * @author zhuyujie
   * @since 2020-6-3
   */
  public void configuratorUnRegister(URL url) {
    try {
      consumer.unRegisterService(url);
    } catch (Exception e) {
      if (e instanceof IllegalStateException) {
        logger.debug("注册中心上不存在需要删除的configurations信息，URL为[" + url + "]");
      } else {
        logger.warn("删除url失败，原因:" + e.getMessage(), e);
      }
    }
  }

  private List<URL> filterUrls(List<URL> searchedUrls, URL sourceUrl, String key) {
    List<URL> filteredUrls = new ArrayList<>(searchedUrls.size());

    String protocol, urlIp, urlServiceName;
    int urlPort;

    for (URL url : searchedUrls) {
      if (url == null) {
        continue;
      }
      // 检验协议名
      protocol = url.getProtocol();
      if (!RegistryConstants.OVERRIDE_PROTOCOL.equals(protocol)) {
        continue;
      }

      // 检验服务名
      urlServiceName = url.getServiceInterface();
      if (!sourceUrl.getServiceInterface().equals(urlServiceName)) {
        continue;
      }

      // 检验IP地址
      urlIp = url.getIp();
      if (StringUtils.isEmpty(urlIp)) {
        continue;
      }
      if (!sourceUrl.getIp().equals(urlIp)) {
        continue;
      }

      // 校验端口号
      urlPort = url.getPort();
      if (sourceUrl.getPort() != urlPort) {
        continue;
      }

      // 是否为当前类处理的参数值
      if (!url.getParameters().containsKey(key)) {
        continue;
      }
      filteredUrls.add(url);
    }
    return filteredUrls;
  }


}
