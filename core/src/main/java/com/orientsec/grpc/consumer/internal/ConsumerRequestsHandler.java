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
import com.orientsec.grpc.common.util.MathUtils;
import com.orientsec.grpc.common.util.Objects;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.consumer.qos.ConsumerRequestsControllerUtils;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 监听客户端对服务端的每秒钟的请求次数参数
 *
 * @author sxp
 * @since 2019/1/30
 */
public class ConsumerRequestsHandler {
  private static final Logger logger = Logger.getLogger(ConsumerRequestsHandler.class.getName());

  // 每秒钟的请求次数
  private static final String KEY = GlobalConstants.Consumer.Key.DEFAULT_REQUESTS;

  private ZookeeperNameResolver zkNameResolver;

  // 是否更新过数据
  private boolean isUpdated = false;

  public ConsumerRequestsHandler(ZookeeperNameResolver zkNameResolver) {
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

    boolean needUpdate = false;
    String newRequestStr = null;

    if (!isEmpty) {
      for (URL url : filteredUrls) {
        newRequestStr = url.getParameter(KEY);

        if (newRequestStr != null) {
          newRequestStr = newRequestStr.trim();
          if (StringUtils.isNotEmpty(newRequestStr) && MathUtils.isLong(newRequestStr)) {
            needUpdate = true;
          }
        }
      }
    }

    /*
     * 如果上一次更新过数据，而本次没有发现数据需要更新，说明参数配置被删除了。
     */
    boolean needResetConfig = false;
    if (isUpdated && !needUpdate) {
      newRequestStr = "0";
      needResetConfig = true;
    }

    if (needUpdate || needResetConfig) {
      if (StringUtils.isNotEmpty(newRequestStr) && MathUtils.isLong(newRequestStr)) {
        long newRequest = Long.parseLong(newRequestStr);

        String serviceName = zkNameResolver.getServiceName();
        long currentRequest = ConsumerRequestsControllerUtils.getMaxRequestsMap(serviceName);

        if (newRequest != currentRequest) {
          ConsumerRequestsControllerUtils.setMaxRequestsMap(serviceName, newRequest);

          if (needUpdate) {
            logger.info("监听到客户端的[每秒钟的请求次数]配置项，参数值为[" + newRequestStr + "]");
          } else {
            logger.info("将客户端的[每秒钟的请求次数]恢复到初始设置[" + newRequestStr + "]");
          }
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
    String consumerIP = zkNameResolver.getConsumerIP();

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
        if (!RegistryConstants.ANYHOST_VALUE.equals(urlIp) && !consumerIP.equals(urlIp)) {
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
