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
import com.orientsec.grpc.provider.task.RegistryTask;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.exception.PropertiesException;
import com.orientsec.grpc.registry.service.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;



/**
 * 对服务是否处于访问保护状态进行处理
 * <pre>
 * 监听目录：/xxx/com.orientsec.[app].[interface].service/configurators
 * 对key为access.protected的override操作做监听
 *
 * 属性access.protected ，表示该服务是否处于访问保护状态，属性的可选值为false 、true ，分别表示不受保护、受保护，
 * 缺省值为false （不受保护）。
 *
 * 如果access.protected为true，写入一条 “rule==> host != ${provider.host.ip}” 的路由规则（临时节点），
 * 表示禁止所有客户端访问当前注册的服务；当服务退出时，由于自动写入的路由规则是临时节点，当关闭与注册中心的连接时，
 * 路由规则自动消失。
 *
 * 如果access.protected为false，不初始化任何路由规则。
 * </pre>
 *
 * @author sxp
 * @since V1.0 2017/4/5
 */
public class AccessProtectedHandler {
  private static final Logger logger = LoggerFactory.getLogger(AccessProtectedHandler.class);
  private String interfaceName;
  private String ip;
  private int port;
  private boolean isLastUpdated = false;// 上一次调用notify是否更新过数据

  public AccessProtectedHandler(String interfaceName, String ip, int port) {
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
    Map<String, Object> serviceConfig;
    Map<String, String> parameters;
    String value;
    boolean accessProtected = false, needUpdate = false;
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
        if (!parameters.containsKey(GlobalConstants.Provider.Key.ACCESS_PROTECTED)) {
          continue;
        }

        value = parameters.get(GlobalConstants.Provider.Key.ACCESS_PROTECTED);
        if (StringUtils.isEmpty(value)) {
          continue;
        }
        accessProtected = Boolean.valueOf(value).booleanValue();

        needUpdate = true;
      }
    }

    /*
     * 如果上一次更新过数据，而本次没有发现数据需要更新，说明访问保护状态的配置信息被删除了。
     * 获取访问保护状态的初始配置，将当前访问保护状态的配置进行重置
     */
    boolean needResetConfig = false;
    if (isLastUpdated && !needUpdate) {
      Map<String, Map<String, Object>> initialServicesConfig = ServiceConfigUtils.getInitialServicesConfig();
      if (initialServicesConfig.containsKey(interfaceName)) {
        serviceConfig = initialServicesConfig.get(interfaceName);

        value = String.valueOf(serviceConfig.get(GlobalConstants.Provider.Key.ACCESS_PROTECTED));
        if (!StringUtils.isEmpty(value)) {
          accessProtected = Boolean.valueOf(value).booleanValue();
          needResetConfig = true;
        }
      }
    }

    if (needUpdate || needResetConfig) {
      // 对全局服务提供者的配置信息做修改
      Map<String, Map<String, Object>> currentServicesConfig = ServiceConfigUtils.getCurrentServicesConfig();
      if (!currentServicesConfig.containsKey(interfaceName)) {
        beforeReturn(needUpdate);
        return;
      }

      serviceConfig = currentServicesConfig.get(interfaceName);

      boolean originalValue;// 原来的配置
      Object obj = serviceConfig.get(GlobalConstants.Provider.Key.ACCESS_PROTECTED);
      if (obj != null) {
        originalValue = Boolean.valueOf(String.valueOf(obj)).booleanValue();
      } else {
        originalValue = false;
      }

      if (accessProtected != originalValue) {
        serviceConfig.put(GlobalConstants.Provider.Key.ACCESS_PROTECTED, String.valueOf(accessProtected));

        Provider provide;

        try {
          provide = new Provider();
        } catch (PropertiesException e) {
          logger.error("AccessProtectedHandler中创建Provider对象时出错", e);
          return;
        }

        URL url = RegistryTask.getAcessProtectdUrl(interfaceName, ip, port);

        if (accessProtected) {
          // 写入禁止所有客户端访问当前注册的服务的路由规则
          provide.registerService(url);
        } else {
          // 删除注册后自动写入的路由规则
          provide.unRegisterService(url);
        }

        if (needUpdate) {
          logger.info("服务提供者[" + interfaceName + "]监听到访问保护状态配置项，参数值为["
                  + accessProtected + "]。");
        } else if (needResetConfig) {
          logger.info("将服务提供者[" + interfaceName + "]的访问保护状态恢复为初始设置["
                  + accessProtected + "]。");
        }
      }
    }

    beforeReturn(needUpdate);
  }

  /**
   * 在返回之前需要调整一些属性值
   *
   * @author sxp
   * @since 2019/1/7
   */
  private void beforeReturn(boolean needUpdate) {
    if (needUpdate) {
      isLastUpdated = true;
    } else {
      isLastUpdated = false;
    }
  }

}
