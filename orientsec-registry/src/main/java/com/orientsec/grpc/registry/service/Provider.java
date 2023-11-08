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

import com.orientsec.grpc.common.collect.ConcurrentHashSet;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.resource.PrivateRegisterCenterConf;
import com.orientsec.grpc.common.resource.RegisterCenterConf;
import com.orientsec.grpc.common.util.IpUtils;
import com.orientsec.grpc.registry.NotifyListener;
import com.orientsec.grpc.registry.Registry;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.common.utils.UrlUtils;
import com.orientsec.grpc.registry.exception.PropertiesException;
import com.orientsec.grpc.registry.remoting.ZookeeperTransporter;
import com.orientsec.grpc.registry.remoting.curator.CuratorZookeeperTransporter;
import com.orientsec.grpc.registry.zookeeper.ZookeeperRegistryFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.orientsec.grpc.common.constant.GlobalConstants.PRIVATE_REGISTRY_CENTER_ADDRESS;
import static com.orientsec.grpc.common.constant.GlobalConstants.SERVICE_TYPE;


public class Provider {
  private ZookeeperRegistryFactory registryFactory = null;
  private URL zkUrl;
  private List<URL> zkUrlList;

  public Provider() throws PropertiesException {
    String key = RegisterCenterConf.getRcProKey();
    zkUrl = UrlUtils.getRegisterURL(key);
    zkUrlList = UrlUtils.getAllProviderRegisterURLs();

    if (zkUrlList == null || zkUrlList.size() == 0) {
      throw new PropertiesException("读取配置文件[" + GlobalConstants.CONFIG_FILE_PATH
              + "]错误，无法获取配置信息");
    }

    init();
  }

  /**
   * 初始化
   */
  public void init() {
    ZookeeperTransporter zookeeperTransporter = new CuratorZookeeperTransporter();
    registryFactory = new ZookeeperRegistryFactory();
    if (registryFactory != null) {
      registryFactory.setZookeeperTransporter(zookeeperTransporter);
    }
  }

  /**
   * 提供服务注册功能
   * <p>
   * 根据URL category属性分别向providers,routers,configurators下写入内容
   * </p>
   *
   * @since 2019-09-30 modify by sxp 完善注册逻辑
   * @since 2019-10-09 modify by sxp 只有服务端注册才考虑多个注册中心、服务类型
   * @since 2019/12/03 modify by wlh registerService与unRegisterService方法代码提取
   */
  public void registerService(URL url) {
    doRegService(url, true);
  }

  /**
   * 提供取消服务注册功能，即从注册中心删除相关信息
   *
   * @param url 服务url
   * @since 2019-09-30 modify by sxp 根据注册逻辑完善注销逻辑
   * @since 2019-10-09 modify by sxp 只有服务端取消注册才考虑多个注册中心、服务类型
   * @since 2019/12/03 modify by wlh registerService与unRegisterService代码提取
   */
  public void unRegisterService(URL url) {
    doRegService(url, false);
  }

  /**
   * registerService与unRegisterService代码逻辑提取封装
   * <p>
   * 增加多套注册service.ip和service.port属性的判断封装
   * </p>
   *
   * @param url       服务url
   * @param isReg     注册/注销服务标识，true：注册服务，false：注销服务
   * @author wlh
   * @since 2019/12/03
   */
  private void doRegService(URL url, boolean isReg){
    String protocol = url.getProtocol();
    if (!RegistryConstants.GRPC_PROTOCOL.equals(protocol)) {
      Registry registry = registryFactory.getRegistry(zkUrl);
      if (isReg) {
        registry.register(url);
      } else {
        registry.unregister(url);
      }
      return;
    }

    Registry registry;
    URL urlOfService;

    String providerRegistryIp = url.getIp();
    int providerRegistryPort = url.getPort();

    Map<String, String> newParameters = new HashMap<>();

    String serviceName = url.getServiceInterface();
    ConcurrentHashSet publicServices = RegisterCenterConf.getPublicServices();
    ConcurrentHashSet privateServices = PrivateRegisterCenterConf.getPrivateServices();

    int publicSize = publicServices.size();
    int privateSize = privateServices.size();

    // public.service.list:如果不配置，表示所有服务都是公共服务
    // private.service.list:如果不配置，将公共服务名称列表之外的服务都视为私有服务

    boolean isAllPublic = false;
    if (publicSize == 0 && privateSize == 0) {
      isAllPublic = true;
    }

    boolean isPublic, isPrivate;
    String registryIp, ip, portStr;
    int registryPort;

    for (URL item : zkUrlList) {
      if (isAllPublic) {
        isPublic = true;
        isPrivate = false;
      } else {
        isPublic = publicServices.contains(serviceName);
        isPrivate = privateServices.contains(serviceName);

        if (isPublic && isPrivate) {
          throw new RuntimeException("配置文件中public.service.list,private.service.list参数值配置错误，" +
                  "服务[" + serviceName + "]不能既是公共服务、又是私有服务。");
        }

        if (!isPublic && !isPrivate) {
          if (publicSize > 0 && privateSize == 0) {
            // 公共列表配置了,私有列表未配置,将公共服务名称列表之外的服务都视为私有服务
            isPrivate = true;
          } else {
            // (1)公共列表未配置,私有列表配置了,但该服务不在私有服务列表中,视为公共服务
            // (2)两者都配置了,但是当前服务不在列表中,视为公共服务
            isPublic = true;
          }
        }
      }

      newParameters.clear();
      newParameters.putAll(url.getParameters());

      registryIp = providerRegistryIp;
      registryPort = providerRegistryPort;

      /**
       * 1. 判断item（zk配置）中是否有包含service.ip和service.port属性(适用于多套注册中心，只有多套注册中心才有该配置)
       * 2. 如果有配置，则更新url的host和port属性
       * 3. 将service.ip和service.port属性添加到newParameters中
       */
      Map<String, String> zkUrlParameter = item.getParameters();
      if (zkUrlParameter.containsKey(GlobalConstants.CommonKey.SERVICE_IP)) {
        ip = zkUrlParameter.get(GlobalConstants.CommonKey.SERVICE_IP);
        if (!IpUtils.isValidIPv4(ip)) {
          throw new RuntimeException("service.ip[" + ip + "]IP格式配置错误。");
        }
        registryIp = ip;
      }

      if(zkUrlParameter.containsKey(GlobalConstants.CommonKey.SERVICE_PORT)){
        portStr = zkUrlParameter.get(GlobalConstants.CommonKey.SERVICE_PORT);
        if (!IpUtils.isValidPort(portStr)) {
          throw new RuntimeException("service.port端口配置错误。");
        }
        registryPort = Integer.parseInt(portStr);
      }

      if (isPrivate) {
        if (PRIVATE_REGISTRY_CENTER_ADDRESS.equals(item.getId())) {
          newParameters.put(SERVICE_TYPE, GlobalConstants.ServiceType.PRIVATE);
        } else {
          // 私有服务不能注册到公共注册中心
          continue;
        }
      } else {
        // 此时isPublic肯定为true
        if (PRIVATE_REGISTRY_CENTER_ADDRESS.equals(item.getId())) {
          // 私有服务不能注册到公共注册中心
          continue;
        } else {
          newParameters.put(SERVICE_TYPE, GlobalConstants.ServiceType.PUBLIC);
        }
      }

      urlOfService = new URL(url.getProtocol(), registryIp, registryPort, newParameters);

      registry = registryFactory.getRegistry(item);
      if (isReg) {
        registry.register(urlOfService);
      } else {
        registry.unregister(urlOfService);
      }
    }
  }

  /**
   * 提供订阅功能，当监控目录内容改变时，进行回调
   *
   * @param url      目标url
   * @param listener 自定义实现的回调函数,需要实现相关的接口
   */
  public void subscribe(URL url, NotifyListener listener) {
    Registry registry = registryFactory.getRegistry(zkUrl);
    registry.subscribe(url, listener);
  }

  /**
   * 取消订阅
   *
   * @param url      目标Url
   * @param listener 自定义实现的回调函数,需要实现相关的接口
   */
  public void unSubscribe(URL url, NotifyListener listener) {
    Registry registry = registryFactory.getRegistry(zkUrl);
    registry.unsubscribe(url, listener);
  }

  /**
   * 提供服务查询功能，需要提供interface以及category属性，默认category为providers
   *
   * @param url 查询条件，支持group,version,classsify等属性进行过滤
   *            例如：grpc://192.168.1.211/com.orientsec.grpc.BarService?version=1.0.0
   *            &interface=com.orientsec.sproc2grpc.service.SprocService
   * @return 所有满足条件的内容（以URL表示）
   */
  public List<URL> lookup(URL url) {
    Registry registry = registryFactory.getRegistry(zkUrl);
    List<URL> urls = registry.lookup(url);
    return urls;
  }

  /**
   * 关闭与注册中心的连接
   */
  public void releaseRegistry() {
    for (URL item : zkUrlList) {
      registryFactory.releaseRegistry(item);
    }
  }

  /**
   * 读取注册中心指定路径节点的数据
   *
   * @param path 路径
   * @return 节点数据
   */
  public String getData(String path) {
    Registry registry = registryFactory.getRegistry(zkUrl);
    return registry.getData(path);
  }
}
