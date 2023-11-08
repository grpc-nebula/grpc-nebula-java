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
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.provider.core.ServiceConfigUtils;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;



/**
 * 对服务是否过期进行处理
 * <pre>
 * 监听目录：/xxx/com.orientsec.[app].[interface].service/configurators
 * 对key为deprecated的override操作做监听，deprecated为true时表示当前服务过期
 * </pre>
 *
 * @author sxp
 * @since V1.0 2017/4/5
 */
public class DeprecatedHandler {
  private static final Logger logger = LoggerFactory.getLogger(DeprecatedHandler.class);
  private String interfaceName;
  private String ip;
  private int port;
  private boolean isLastUpdated = false;// 上一次调用notify是否更新过数据

  public DeprecatedHandler(String interfaceName, String ip, int port) {
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
    Map<String, Map<String, Object>> currentServicesConfig;
    Map<String, Object> serviceConfig;
    Map<String, String> parameters;
    String value;
    boolean deprecated = false, needUpdate = false;
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
        if (!parameters.containsKey(GlobalConstants.Provider.Key.DEPRECATED)) {
          continue;
        }

        // 是否过期
        value = parameters.get(GlobalConstants.Provider.Key.DEPRECATED);
        if (StringUtils.isEmpty(value)) {
          continue;
        }
        deprecated = Boolean.valueOf(value).booleanValue();

        // 在循环外做对属性进行调整
        needUpdate = true;
      }
    }

    /*
     * 如果上一次更新过数据，而本次没有发现数据需要更新，说明deprecated的配置信息被删除了。
     * 获取deprecated的初始配置，将当前deprecated的配置进行重置
     */
    boolean needResetConfig = false;
    if (isLastUpdated && !needUpdate) {
      Map<String, Map<String, Object>> initialServicesConfig = ServiceConfigUtils.getInitialServicesConfig();
      if (initialServicesConfig.containsKey(interfaceName)) {
        serviceConfig = initialServicesConfig.get(interfaceName);

        value = String.valueOf(serviceConfig.get(GlobalConstants.Provider.Key.DEPRECATED));
        if (!StringUtils.isEmpty(value)) {
          deprecated = Boolean.valueOf(value).booleanValue();
          needResetConfig = true;
        }
      }
    }

    if (needUpdate || needResetConfig) {
      // 对全局服务提供者的配置信息做修改
      currentServicesConfig = ServiceConfigUtils.getCurrentServicesConfig();
      if (currentServicesConfig.containsKey(interfaceName)) {
        serviceConfig = currentServicesConfig.get(interfaceName);
        serviceConfig.put(GlobalConstants.Provider.Key.DEPRECATED, String.valueOf(deprecated));
      }

      if (needUpdate) {
        logger.info("服务提供者[" + interfaceName + "]监听到服务过期配置项，参数值为["
                + deprecated + "]。");
      } else if (needResetConfig) {
        logger.info("将服务提供者[" + interfaceName + "]的服务过期恢复为初始设置["
                + deprecated + "]。");
      }
    }

    if (needUpdate) {
      isLastUpdated = true;
    } else {
      isLastUpdated = false;
    }
  }
}
