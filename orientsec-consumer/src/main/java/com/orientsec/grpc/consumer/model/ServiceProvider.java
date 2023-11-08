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


package com.orientsec.grpc.consumer.model;


import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.common.utils.StringUtils;

/**
 * 服务提供者
 */
public class ServiceProvider {
  private String host;
  private int port = 80;
  private String version;
  private String group;
  private int timeout = 1000;
  private int reties = 2;
  private int connections = 0;
  private GlobalConstants.LB_STRATEGY loadblance = GlobalConstants.LB_STRATEGY.PICK_FIRST;
  private boolean async = false;
  private String token;
  private boolean deprecated = false;
  private boolean dynamic = true;
  private String accesslog;
  private String owner;
  private int weight = 100;
  private GlobalConstants.CLUSTER cluster = GlobalConstants.CLUSTER.FAILOVER;
  private String application;
  private String applicationVersion;
  private String organization;
  private String environment;
  private String module;
  private String moduleVerison;
  private boolean anyhost = false;
  private String interfaceName;// 接口名称
  private String methods;
  private long pid;
  private String side;
  private long timestamp;
  private String grpc;
  private URL url;
  private boolean master = true;
  /**
   * 获取URL
   *
   * @author sxp
   * @since 2018/12/1
   */
  public URL getUrl() {
    return url;
  }
  /**
   * 设置URL
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setUrl(URL url) {
    this.url = url;
  }

  public ServiceProvider(){
    this.anyhost = false;
    this.async = false;
    this.cluster = GlobalConstants.CLUSTER.FAILOVER;
    this.connections = 0;
    this.deprecated = false;
    this.dynamic = false;
    this.reties = 2;
    this.loadblance = GlobalConstants.LB_STRATEGY.PICK_FIRST;
    this.timeout = 1000;
    this.weight = 100;
    this.side = RegistryConstants.PROVIDER_SIDE;
  }

  public ServiceProvider fromURL(URL url, Object currentMasterValue,Object currentGroupValue){
    this.url = url;
    this.host = url.getIp();
    this.port = url.getPort(80);
    this.version = url.getParameter(GlobalConstants.Provider.Key.VERSION);
    this.interfaceName = url.getParameter(GlobalConstants.Provider.Key.INTERFACE);
    this.loadblance = GlobalConstants.string2LB(url.getParameter(
            GlobalConstants.Provider.Key.DEFAULT_LOADBALANCE));
    this.cluster = GlobalConstants.string2Cluster(url.getParameter(
            GlobalConstants.Provider.Key.DEFAULT_CLUSTER));
    String weight = url.getParameter(GlobalConstants.Provider.Key.WEIGHT);
    if (weight != null && weight.length() > 0){
      this.weight = StringUtils.parseInteger(weight);
    }
    String deprecated = url.getParameter(GlobalConstants.Provider.Key.DEPRECATED);
    this.deprecated = "true".compareToIgnoreCase(deprecated) == 0 ? true: false;
    this.master = Boolean.valueOf(url.getParameter(GlobalConstants.Provider.Key.MASTER));
    if (currentMasterValue != null && (currentMasterValue instanceof Boolean)) {
      this.master = ((Boolean) currentMasterValue).booleanValue();
    }
    this.group = url.getParameter(GlobalConstants.Provider.Key.GROUP);
    if(currentGroupValue != null && (currentGroupValue instanceof String)){
      this.group = currentGroupValue.toString();
    }
    this.application = url.getParameter(GlobalConstants.Provider.Key.APPLICATION);

    return this;
  }

  /**
   * 获取主机
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getHost() {
    return host;
  }

  /**
   * 设置主机
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setHost(String host) {
    this.host = host;
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

  /**
   * 获取版本
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getVersion() {
    return version;
  }

  /**
   * 设置版本
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * 获取分组
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getGroup() {
    return group;
  }

  /**
   * 设置分组
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setGroup(String group) {
    this.group = group;
  }

  /**
   * 获取超时时间
   *
   * @author sxp
   * @since 2018/12/1
   */
  public int getTimeout() {
    return timeout;
  }

  /**
   * 设置超时时间
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  /**
   * 获取重试次数
   *
   * @author sxp
   * @since 2018/12/1
   */
  public int getReties() {
    return reties;
  }

  /**
   * 设置失败重试次数
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setReties(int reties) {
    this.reties = reties;
  }

  /**
   * 获取连接数
   *
   * @author sxp
   * @since 2018/12/1
   */
  public int getConnections() {
    return connections;
  }

  /**
   * 设置连接数
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setConnections(int connections) {
    this.connections = connections;
  }

  public GlobalConstants.LB_STRATEGY getLoadblance() {
    return loadblance;
  }

  /**
   * 设置负载均衡策略
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setLoadblance(GlobalConstants.LB_STRATEGY loadblance) {
    this.loadblance = loadblance;
  }

  /**
   * 是否异步执行
   *
   * @author sxp
   * @since 2018/12/1
   */
  public boolean isAsync() {
    return async;
  }

  /**
   * 设置是否异步执行
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setAsync(boolean async) {
    this.async = async;
  }

  /**
   * 获取令牌
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getToken() {
    return token;
  }

  /**
   * 设置令牌
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setToken(String token) {
    this.token = token;
  }

  /**
   * 服务是否过期
   *
   * @author sxp
   * @since 2018/12/1
   */
  public boolean isDeprecated() {
    return deprecated;
  }

  /**
   * 设置服务是否过去
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setDeprecated(boolean deprecated) {
    this.deprecated = deprecated;
  }

  /**
   * 是否动态注册
   *
   * @author sxp
   * @since 2018/12/1
   */
  public boolean isDynamic() {
    return dynamic;
  }

  /**
   * 设置是否动态注册
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setDynamic(boolean dynamic) {
    this.dynamic = dynamic;
  }

  /**
   * 获取访问日志的路径
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getAccesslog() {
    return accesslog;
  }

  /**
   * 设置访问日志的路径
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setAccesslog(String accesslog) {
    this.accesslog = accesslog;
  }

  /**
   * 获取服务所有者
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getOwner() {
    return owner;
  }

  /**
   * 设置服务所有者
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setOwner(String owner) {
    this.owner = owner;
  }

  /**
   * 获取权重
   *
   * @author sxp
   * @since 2018/12/1
   */
  public int getWeight() {
    return weight;
  }

  /**
   * 设置权重
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setWeight(int weight) {
    this.weight = weight;
  }

  public GlobalConstants.CLUSTER getCluster() {
    return cluster;
  }

  /**
   * 设置集群方式
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setCluster(GlobalConstants.CLUSTER cluster) {
    this.cluster = cluster;
  }

  /**
   * 获取应用名称
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getApplication() {
    return application;
  }

  /**
   * 设置应用名称
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setApplication(String application) {
    this.application = application;
  }

  /**
   * 获取应用版本号
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getApplicationVersion() {
    return applicationVersion;
  }

  /**
   * 设置应用版本号
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setApplicationVersion(String applicationVersion) {
    this.applicationVersion = applicationVersion;
  }

  /**
   * 获取所属组织
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * 设置所属组织
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setOrganization(String organization) {
    this.organization = organization;
  }

  /**
   * 获取运行环境
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getEnvironment() {
    return environment;
  }

  /**
   * 设置运行环境
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  /**
   * 获取所属模块
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getModule() {
    return module;
  }

  /**
   * 设置所属模块
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setModule(String module) {
    this.module = module;
  }

  /**
   * 获取模块版本
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getModuleVerison() {
    return moduleVerison;
  }

  /**
   * 设置模块版本号
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setModuleVerison(String moduleVerison) {
    this.moduleVerison = moduleVerison;
  }

  /**
   * 是否对所有IP生效
   *
   * @author sxp
   * @since 2018/12/1
   */
  public boolean isAnyhost() {
    return anyhost;
  }

  /**
   * 设置是否对所有IP生效
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setAnyhost(boolean anyhost) {
    this.anyhost = anyhost;
  }

  /**
   * 获取服务接口名称
   */
  public String getInterfaceName() {
    return interfaceName;
  }

  /**
   * 设置服务接口名称
   */
  public void setInterfaceName(String interfaceName) {
    this.interfaceName = interfaceName;
  }

  /**
   * 获取服务所支持的方法
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getMethods() {
    return methods;
  }

  /**
   * 设置服务所支持的方法
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setMethods(String methods) {
    this.methods = methods;
  }

  /**
   * 获取进程ID
   *
   * @author sxp
   * @since 2018/12/1
   */
  public long getPid() {
    return pid;
  }

  /**
   * 设置进程ID
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setPid(long pid) {
    this.pid = pid;
  }

  /**
   * 获取属于哪一方（提供者、消费者）
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getSide() {
    return side;
  }

  /**
   * 设置属于哪一方
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setSide(String side) {
    this.side = side;
  }

  /**
   * 获取时间戳
   *
   * @author sxp
   * @since 2018/12/1
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * 设置时间戳
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * 获取grpc的版本号
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getGrpc() {
    return grpc;
  }

  /**
   * 设置grpc的版本号
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setGrpc(String grpc) {
    this.grpc = grpc;
  }

  /**
   * 获取master值
   * @return
   */
  public boolean getMaster() {
    return master;
  }

  /**
   * 设置master值
   * @param master
   */
  public void setMaster(boolean master) {
    this.master = master;
  }
}
