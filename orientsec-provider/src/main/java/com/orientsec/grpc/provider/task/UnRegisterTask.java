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
package com.orientsec.grpc.provider.task;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.provider.core.ProviderServiceRegistryImpl;
import com.orientsec.grpc.provider.core.ServiceConfigUtils;
import com.orientsec.grpc.provider.watch.ProvidersListener;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.service.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 注销服务
 *
 * @author sxp
 * @since V1.0 2017/3/23
 */
public class UnRegisterTask {
  private static final Logger logger = LoggerFactory.getLogger(UnRegisterTask.class);
  private ProviderServiceRegistryImpl caller;

  public UnRegisterTask(ProviderServiceRegistryImpl caller, List<Map<String, Object>> servicesParams) {
    this.caller = caller;
  }

  /**
   * 主方法
   *
   * @author sxp
   * @since V1.0 2017/3/23
   */
  public void work() throws Exception {
    List<Map<String, Object>> servicesConfig = caller.getServicesConfig();
    Map<String, Map<String, Object>> currentServicesConfig = ServiceConfigUtils.getCurrentServicesConfig();
    Map<String, Map<String, Object>> initialServicesConfig = ServiceConfigUtils.getInitialServicesConfig();
    List<Map<String, Object>> listenersInfo = caller.getListenersInfo();

    String[] keysWillDel = new String[servicesConfig.size()];

    String interfaceName;
    int index = 0;

    for (Map<String, Object> config : servicesConfig) {
      interfaceName = (String) config.get(GlobalConstants.Provider.Key.INTERFACE);
      keysWillDel[index++] = interfaceName;
    }

    // 从内存中删除相应服务配置信息(所有服务提供者的配置信息)
    for (String name : keysWillDel) {
      currentServicesConfig.remove(name);
      initialServicesConfig.remove(name);
    }

    Provider provider;
    ProvidersListener listener;
    URL urlOfService, urlOfListener;

    provider = new Provider();

    // 注销监听器、注销服务
    for (Map<String, Object> info : listenersInfo) {
      urlOfService = (URL) info.get("url-service");
      urlOfListener = (URL) info.get("url-listener");
      listener = (ProvidersListener) info.get("listener");

      // 注销监听器(configurators)
      logger.info("服务端注销监听器");
      provider.unSubscribe(urlOfListener, listener);

      // 不管是否为动态注册，都进行注销服务操作
      logger.info("服务端注销" + urlOfService);
      provider.unRegisterService(urlOfService);
    }

    // 从内存中删除相应服务配置信息(当前服务器的配置信息)
    servicesConfig.clear();
  }

}
