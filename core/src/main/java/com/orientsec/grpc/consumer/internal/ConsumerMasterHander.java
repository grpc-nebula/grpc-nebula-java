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
package com.orientsec.grpc.consumer.internal;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 监听客户端调用主还是备
 */
public class ConsumerMasterHander {

  private static final Logger logger = LoggerFactory.getLogger(ConsumerMasterHander.class);

  // 客户端invoke.master,代表当前客户端流量走主还是走备
  private static final String KEY = GlobalConstants.Consumer.Key.CONSUMER_MASTER;

  private final ZookeeperNameResolver zkNameResolver;

  // 是否更新过数据
  private boolean isUpdated = false;

  public ConsumerMasterHander(ZookeeperNameResolver zkNameResolver) {
    this.zkNameResolver = zkNameResolver;
  }

  public void notify(List<URL> urls) {
    List<URL> filteredUrls = filterUrls(urls);

    boolean isEmpty = false;
    if (filteredUrls.isEmpty()) {
      isEmpty = true;
    } else if (filteredUrls.size() == 1 && Constants.EMPTY_PROTOCOL.equals(filteredUrls.get(0).getProtocol())) {
      isEmpty = true;
    }

    // 当前客户端调用的是主还是备
    boolean currentInvokeMaster = zkNameResolver.getInvokeMaster();
    boolean needUpdate = false;
    boolean newInvokeMaster = currentInvokeMaster;

    if (!isEmpty) {
      for (URL url : filteredUrls) {
        String masterStr = url.getParameter(KEY);
        newInvokeMaster = Boolean.parseBoolean(masterStr);
        if (currentInvokeMaster != newInvokeMaster) {
          needUpdate = true;
        }
      }
    }

    /*
     * 如果上一次更新过数据，而本次没有发现数据需要更新，说明参数配置被删除了。
     */
    boolean needResetConfig = false;
    if (isUpdated && !needUpdate) {
      newInvokeMaster = true;
      needResetConfig = true;
    }

    if (needUpdate || needResetConfig) {

      if (currentInvokeMaster != newInvokeMaster) {
        zkNameResolver.setInvokeMaster(newInvokeMaster);
        String serviceName = zkNameResolver.getServiceName();

        // 重新选择服务提供者
        Object lock = zkNameResolver.getLock();
        synchronized (lock) {
          zkNameResolver.getAllByName(serviceName);
          try {
            logger.info("重选服务提供者");
            zkNameResolver.resolveServerInfo(null, null);
          } catch (Throwable t) {
            logger.error("重选服务提供者出错", t);
          }
        }

        if (needUpdate) {
          logger.info("监听到客户端的[invoke.master]配置项，参数值为[" + newInvokeMaster + "]");
        } else {
          logger.info("将客户端的[invoke.master]恢复到初始设置[" + newInvokeMaster + "]");
        }
      }
    }

    if (needUpdate) {
      isUpdated = true;
    } else {
      isUpdated = false;
    }
  }

  private List<URL> filterUrls(List<URL> urls) {
    List<URL> filteredUrls = new ArrayList<>(urls.size());

    String protocol, urlIp;
    int urlPort;

    for (URL url : urls) {
      if (url == null) {
        continue;
      }

      if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
        // 对于节点删除的情况也要进行处理
      } else {
        // 目前只对override操作做监听
        protocol = url.getProtocol();
        if (!RegistryConstants.OVERRIDE_PROTOCOL.equals(protocol)) {
          continue;
        }

        // 检验IP地址
        urlIp = url.getIp();
        if (StringUtils.isEmpty(urlIp)) {
          continue;
        }
        // 切换调用主备的配置仅限全局配置
        if (!RegistryConstants.ANYHOST_VALUE.equals(urlIp)) {
          continue;
        }

        // 校验端口号
        urlPort = url.getPort();
        if (urlPort != 0) {
          continue;
        }

        // 是否为当前类处理的参数值
        if (!url.getParameters().containsKey(KEY)) {
          continue;
        }
      }

      filteredUrls.add(url);
    }

    return filteredUrls;
  }


}
