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
package com.orientsec.grpc.common.model;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基本的服务提供者实体类
 *
 * @author sxp
 * @since 2018/8/11
 */
public class BasicProvider {
  private String serviceName;
  private String ip;
  private int port;
  private ConcurrentHashMap<String, Object> properties = new ConcurrentHashMap<>();
  private ConcurrentHashMap<String, Object> initProperties = new ConcurrentHashMap<>();

  public BasicProvider(String serviceName, String ip, int port) {
    this.serviceName = serviceName;
    this.ip = ip;
    this.port = port;
  }

  /**
   * 获取服务接口名称
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * 设置服务接口名称
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * 获取IP
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getIp() {
    return ip;
  }

  /**
   * 设置IP
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setIp(String ip) {
    this.ip = ip;
  }

  /**
   * 获取端口
   *
   * @author sxp
   * @since 2018/12/1
   */
  public int getPort() {
    return port;
  }

  /**
   * 设置端口
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setPort(int port) {
    this.port = port;
  }

  public ConcurrentHashMap<String, Object> getProperties() {
    return properties;
  }

  public ConcurrentHashMap<String, Object> getInitProperties() {
    return initProperties;
  }
}
