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
package com.orientsec.grpc.provider.watch;

import com.google.common.base.Preconditions;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.util.MathUtils;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.provider.core.ServiceConfigUtils;
import com.orientsec.grpc.provider.qos.ProviderRequestsControllerUtils;
import com.orientsec.grpc.provider.qos.RequestsController;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;



/**
 * 监听服务提供者请求数参数的变化
 * <p>
 * 对服务请求数进行处理。
 * 用于控制Provider端同一时间点允许响应的最大请求
 * 监听目录：/xxx/com.orientsec.[app].[interface].service/configurators
 * 对key为default.requests的override操作做监听，default.requests表示每个provider最大并发请求数
 * </p>
 *
 * @since 2018/5/31
 */
public class RequestsHandler {
  private static final Logger logger = LoggerFactory.getLogger(RequestsHandler.class);
  private String interfaceName;
  private String ip;
  private int port;
  private boolean isLastUpdated = false;// 上一次调用notify是否更新过数据

  public RequestsHandler(String interfaceName, String ip, int port) {
    Preconditions.checkNotNull(interfaceName, "interfaceName");
    Preconditions.checkNotNull(ip, "ip");
    this.interfaceName = interfaceName;
    this.ip = ip;
    this.port = port;
  }

  /**
   * 数据发生变化的处理逻辑
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void notify(List<URL> urls) {
    ConcurrentHashMap<String, RequestsController> controllers;
    Map<String, Map<String, Object>> currentServicesConfig;
    Map<String, Object> serviceConfig;
    Map<String, String> parameters;
    RequestsController controller;
    RequestsController oldValue;
    String requests;
    int requestsNum = GlobalConstants.Provider.DEFAULT_REQUESTS_NUM;
    boolean needUpdate = false;
    boolean isEmptyProtocol = false;

    String urlIp;
    int urlPort;

    // 向注册中心订阅监听器成功后，如果监听节点下没有子节点，会立即返回一个协议为empty的URL
    Objects.requireNonNull(urls);
    if (urls.size() == 1 && Constants.EMPTY_PROTOCOL.equals(urls.get(0).getProtocol())) {
      isEmptyProtocol = true;
    }

    if (!isEmptyProtocol) {
      for (URL url : urls) {

        urlIp = url.getIp();
        if (StringUtils.isEmpty(urlIp)) {
          continue;
        }
        if (!RegistryConstants.ANYHOST_VALUE.equals(urlIp) && !ip.equals(urlIp)) {
          continue;
        }

        urlPort = url.getPort();
        if (port != urlPort) {
          continue;
        }

        parameters = url.getParameters();
        if (!parameters.containsKey(GlobalConstants.Provider.Key.DEFAULT_REQUESTS)) {
          continue;
        }

        // 取连接数
        requests = parameters.get(GlobalConstants.Provider.Key.DEFAULT_REQUESTS);

        if (MathUtils.isInteger(requests)) {
          requestsNum = Integer.parseInt(requests);
          requestsNum = RequestsController.getValidMax(requestsNum);
          needUpdate = true;
        }
      }
    }

    /*
     * 如果上一次更新过数据，而本次没有发现数据需要更新，说明连接数的配置信息被删除了。
     * 获取连接数的初始配置，将当前连接数的配置进行重置
     */
    boolean needResetConfig = false;
    if (isLastUpdated && !needUpdate) {
      Map<String, Map<String, Object>> initialServicesConfig = ServiceConfigUtils.getInitialServicesConfig();
      if (initialServicesConfig.containsKey(interfaceName)) {
        serviceConfig = initialServicesConfig.get(interfaceName);

        requests = String.valueOf(serviceConfig.get(GlobalConstants.Provider.Key.DEFAULT_REQUESTS));
        if (MathUtils.isInteger(requests)) {
          requestsNum = Integer.parseInt(requests);
          requestsNum = RequestsController.getValidMax(requestsNum);
        }
        needResetConfig = true;
      }
    }

    if (needUpdate || needResetConfig) {
      controllers = ProviderRequestsControllerUtils.getControllers();
      if (controllers.containsKey(interfaceName)) {
        controller = controllers.get(interfaceName);
      } else {
        controller = new RequestsController(requestsNum);
        oldValue = controllers.putIfAbsent(interfaceName, controller);
        if (oldValue != null) {
          controller = oldValue;
        }
      }
      controller.setMax(requestsNum);

      currentServicesConfig = ServiceConfigUtils.getCurrentServicesConfig();
      if (currentServicesConfig.containsKey(interfaceName)) {
        serviceConfig = currentServicesConfig.get(interfaceName);
        serviceConfig.put(GlobalConstants.Provider.Key.DEFAULT_REQUESTS, String.valueOf(requestsNum));
      }

      if (needUpdate) {
        logger.info("服务提供者[" + interfaceName + "]监听到最大并发请求数配置项，参数值为["
                + requestsNum + "]。");
      } else if (needResetConfig) {
        logger.info("将服务提供者[" + interfaceName + "]的最大并发请求数恢复到初始设置["
                + requestsNum + "]。");
      }
    }

    if (needUpdate) {
      isLastUpdated = true;
    } else {
      isLastUpdated = false;
    }
  }
}
