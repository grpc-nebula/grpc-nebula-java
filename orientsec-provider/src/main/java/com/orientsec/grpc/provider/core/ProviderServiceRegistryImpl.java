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
package com.orientsec.grpc.provider.core;

import com.orientsec.grpc.common.exception.BusinessException;
import com.orientsec.grpc.common.resource.SystemSwitch;
import com.orientsec.grpc.common.util.IpUtils;
import com.orientsec.grpc.provider.task.RegistryTask;
import com.orientsec.grpc.provider.task.UnRegisterTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;



/**
 * 默认的服务注册
 * <p>
 * 1.服务提供者启动服务时，自动注册该服务 <br>
 * 2.服务提供者停止服务时，自动注销服务
 * </p>
 *
 * @author sxp
 * @since 2018/3/20
 */
public class ProviderServiceRegistryImpl implements ProviderServiceRegistry {
  private static final Logger logger = LoggerFactory.getLogger(ProviderServiceRegistryImpl.class);

  /**
   * 当前服务器上所有服务的的配置
   */
  private List<Map<String, Object>> servicesConfig;

  /**
   * 当前服务器上的注册url和监听器
   */
  private List<Map<String, Object>> listenersInfo;

  /**
   * 服务器的IP（本地的IP）
   */
  private String ip;

  /**
   * 服务的版本
   */
  private String version;

  /**
   * 注册服务
   *
   * @author sxp
   * @since V1.0 2017/3/20
   */
  @Override
  public void register(List<Map<String, Object>> servicesParams) throws BusinessException {
    if (!SystemSwitch.PROVIDER_ENABLED) {
      return;
    }

    servicesConfig = new ArrayList<Map<String, Object>>(servicesParams.size());
    listenersInfo = new ArrayList<Map<String, Object>>(servicesParams.size());
    ip = IpUtils.getIP4WithPriority();

    RegistryTask registerTask = new RegistryTask(this, servicesParams);
    try {
     registerTask.work();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new BusinessException(e.getMessage(), e);
    }
  }

  /**
   * 注销服务
   *
   * @param servicesParams 服务的属性
   * @author sxp
   * @since V1.0 2017-3-30
   */
  @Override
  public void unRegister(List<Map<String, Object>> servicesParams) throws BusinessException {
    if (!SystemSwitch.PROVIDER_ENABLED) {
      return;
    }

    UnRegisterTask unRegisterTask = new UnRegisterTask(this, servicesParams);

    try {
      unRegisterTask.work();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new BusinessException(e.getMessage(), e);
    }
  }

  // ---- getters and setters ----

  /**
   * 获取配置信息
   *
   * @author sxp
   * @since 2018/12/1
   */
  public List<Map<String, Object>> getServicesConfig() {
    return servicesConfig;
  }

  /**
   * 获取监听器信息
   *
   * @author sxp
   * @since 2018/12/1
   */
  public List<Map<String, Object>> getListenersInfo() {
    return listenersInfo;
  }

  /**
   * 获取服务提供者的IP
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getIp() {
    return ip;
  }

  /**
   * 获取服务版本
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getVersion() {
    return version;
  }

  /**
   * 设置服务版本
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setVersion(String version) {
    this.version = version;
  }
}
