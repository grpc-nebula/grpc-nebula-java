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


package com.orientsec.grpc.registry.service;

import com.orientsec.grpc.common.resource.RegisterCenterConf;
import com.orientsec.grpc.registry.NotifyListener;
import com.orientsec.grpc.registry.Registry;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.common.utils.UrlUtils;
import com.orientsec.grpc.registry.exception.PropertiesException;
import com.orientsec.grpc.registry.remoting.ZookeeperTransporter;
import com.orientsec.grpc.registry.remoting.curator.CuratorZookeeperTransporter;
import com.orientsec.grpc.registry.zookeeper.ZookeeperRegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class Consumer {
  private static Logger logger = LoggerFactory.getLogger(Consumer.class);

  private ZookeeperRegistryFactory registryFactory = null;
  private URL url;

  public void setUrl(URL url) {
    this.url = url;
  }

  public Consumer(URL url) {
    //这个url是要注册目标url
    this.url = url;
    init();
  }

  public Consumer() throws PropertiesException {
    // 配置文件中非空注册中心的key值
    String key = RegisterCenterConf.getRcProKey();

    this.url = UrlUtils.getRegisterURL(key);
    if (this.url == null) {
      String msg = "无法获取注册中心地址配置信息";
      logger.error(msg);
      throw new PropertiesException(msg);
    }
    init();
  }

  /**
   * 初始化.
   */
  public void init() {
    ZookeeperTransporter zookeeperTransporter = new CuratorZookeeperTransporter();
    registryFactory = new ZookeeperRegistryFactory();
    if (registryFactory != null) {
      registryFactory.setZookeeperTransporter(zookeeperTransporter);
    }

  }

  /**
   * 提供服务注册功能，分别向providers,routers,configurators下写入内容
   *
   * @param url 注册url，provider,router,configurator均使用该url，通过category注册到不同目录下
   */
  public void registerService(URL url) {
    Registry registry = registryFactory.getRegistry(this.url);
    registry.register(url);
  }

  /**
   * 提供取消客户端注册功能，即从注册中心删除相关信息
   *
   * @param url 服务url
   */
  public void unRegisterService(URL url) {
    Registry registry = registryFactory.getRegistry(this.url);
    registry.unregister(url);
  }

  /**
   * 提供服务订阅功能
   *
   * @param url      服务url路径，主要提供interface以及category属性，默认category为providers
   * @param listener 用户自定义的监听接口，当监听目录下内容变化时，返回所有子目录内容
   */
  public void subscribe(URL url, NotifyListener listener) {
    Registry registry = registryFactory.getRegistry(this.url);
    registry.subscribe(url, listener);
  }

  /**
   * 取消订阅
   *
   * @param url      目标Url
   * @param listener 自定义实现的回调函数,需要实现相关的接口
   */
  public void unSubscribe(URL url, NotifyListener listener) {
    Registry registry = registryFactory.getRegistry(this.url);
    registry.unsubscribe(url, listener);
  }

  /**
   * 提供服务查询功能，需要提供interface以及category属性，默认category为providers
   *
   * @param url 查询条件，支持group,version,classsify等属性进行过滤
   *            例如：provider://192.168.1.211/com.orientsec.grpc.BarService?version=1.0.0
   *            &interface=com.orientsec.sproc2grpc.service.SprocService
   * @return 所有满足条件的内容（以URL表示）
   */
  public List<URL> lookup(URL url) {
    Registry registry = registryFactory.getRegistry(this.url);
    return registry.lookup(url);
  }

  /**
   * 读取注册中心指定路径节点的数据
   *
   * @param path 路径
   * @return 节点数据
   */
  public String getData(String path) {
    Registry registry = registryFactory.getRegistry(this.url);
    return registry.getData(path);
  }

  /**
   * 关闭与注册中心的连接
   */
  public void releaseRegistry() {
    registryFactory.releaseRegistry(this.url);
  }
}
