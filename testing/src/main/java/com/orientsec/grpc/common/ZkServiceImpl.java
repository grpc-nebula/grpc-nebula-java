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
package com.orientsec.grpc.common;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.util.IpUtils;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.exception.PropertiesException;
import com.orientsec.grpc.registry.service.Provider;
import com.orientsec.grpc.server.SingleServiceServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * zookeeper服务实现类
 * <p>
 * 基于dfzq-grpc-java中的一些工具类
 * </p>
 *
 * @author sxp
 * @since 2018/7/20
 */
public class ZkServiceImpl extends Provider {

  /**
   * zookeeper的服务器地址配置在dfzq-grpc-java-config.properties中
   */
  public ZkServiceImpl() throws PropertiesException {
    super();
  }

  /**
   * 服务查询
   * <p>
   * 需要提供interface以及category属性，默认category为providers
   * </p>
   *
   * @param url 查询条件，支持group,version,classsify等属性进行过滤。
   *            例如：grpc://192.168.1.211/com.orientsec.grpc.BarService?version=1.0.0&interface=com.orientsec.sproc2grpc.service.SprocService
   * @return 所有满足条件的内容（以URL表示）
   */
  public List<URL> lookup(URL url) {
    return super.lookup(url);
  }

  /**
   * 服务注册
   * <p>
   * 根据URL category属性分别向providers,routers,configurators下写入内容
   * </p>
   *
   * @param url 注册url，provider,router,configurator均使用该url，通过category注册到不同目录下
   */
  public void registerService(URL url) {
    super.registerService(url);
  }

  /**
   * 注销服务
   */
  public void unRegisterService(URL url){
    super.unRegisterService(url);
  }


  /**
   * 修改一个服务
   * <p>
   * 实现方法：先删除后插入
   * <p/>
   *
   * @param originalrUrl 原始服务的URL
   * @param newUrl 修成后的URL
   */
  public void modifyService(URL originalrUrl, URL newUrl) {
    unRegisterService(originalrUrl);
    registerService(newUrl);
  }

  /**
   * 关闭与zookeeper的连接
   * <p>
   * 会自动删除已注册和已订阅的服务
   * </p>
   */
  public void close() {
    super.releaseRegistry();
  }


  public static void main(String[] args) throws Exception {
    SingleServiceServer server = new SingleServiceServer();

    String ip = IpUtils.getLocalHostAddress();
    String serviceName = server.getServiceName();

    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(GlobalConstants.Consumer.Key.INTERFACE, serviceName);
    parameters.put(GlobalConstants.CommonKey.CATEGORY, RegistryConstants.PROVIDERS_CATEGORY);
    URL url = new URL(RegistryConstants.GRPC_PROTOCOL, ip, server.getPort(), parameters);

    ZkServiceImpl zkService = new ZkServiceImpl();
    zkService.registerService(url);

    List<URL> urls = zkService.lookup(url);

    if (urls == null || urls.isEmpty()) {
      System.out.println("lookup查询结果为空");
    } else {
      for (URL item : urls) {
        System.out.println(item);
        System.out.println("IP地址为：" + item.getHost());
        System.out.println("端口为：" + item.getPort());
      }
    }
  }


}
