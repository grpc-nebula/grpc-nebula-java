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
import com.orientsec.grpc.registry.NotifyListener;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.common.UrlIpComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;



/**
 * 服务提供者监听器
 * <pre>
 * 该监听器的监听目录是：/xxx/com.orientsec.[app].[interface].service/configurators
 *
 * 一个configurator对象表示平台对服务的参数的配置调整，通常用户禁止服务provider、调整权重、调整调度算法、服务降级等。
 * url格式如下：
 * override://0.0.0.0[:1111]/com.foo.BarService?category=configurators
 *          &dynamic=false&application=foo&key=value[&key=value]
 *
 * override 表示数据采用覆盖方式，支持override和absent，可扩展，必填。
 * 0.0.0.0  表示对所有IP地址生效，如果只想覆盖某个IP的数据，请填入具体IP，必填。
 * 1111     端口，服务端的端口
 * com.foo.BarService 表示只对指定服务生效，必填。
 * category=configurators 表示该数据为动态配置类型，必填。
 * dynamic=false  表示该数据为持久数据，当注册方退出时，数据依然保存在注册中心，必填。
 * application=foo  表示只对指定应用生效，可不填，表示对所有应用生效。
 * key=value  表示将满足以上条件的名称为key参数的值覆盖为值为value
 * </pre>
 *
 * @author sxp
 * @since V1.0 2017/3/27
 */
public class ProvidersListener implements NotifyListener {
  private static final Logger logger = LoggerFactory.getLogger(ProvidersListener.class);
  private String interfaceName;
  private String ip;
  private int port;
  private String application;
  private RequestsHandler requestsHandler;
  private ConnectionsHandler connectionsHandler;
  private DeprecatedHandler deprecatedHandler;
  private AccessProtectedHandler accessProtectedHandler;

  public ProvidersListener(String interfaceName, String ip, String application, int port) {
    Preconditions.checkNotNull(interfaceName, "interfaceName");
    Preconditions.checkNotNull(ip, "ip");
    Preconditions.checkNotNull(application, "application");
    this.interfaceName = interfaceName;
    this.ip = ip;
    this.application = application;
    this.port = port;
  }

  /**
   * 数据发生变化的处理逻辑
   *
   * @author sxp
   * @since 2018/12/1
   */
  @Override
  public void notify(List<URL> urls) {
    // 过滤url
    List<URL> filteredUrls = filterUrls(urls);
    if (filteredUrls.isEmpty()) {
      return;
    }

    // 排序
    filteredUrls = sort(filteredUrls);

    /**
     * 对服务并发请求数default.requests进行处理
     */
    if (requestsHandler == null) {
      requestsHandler = new RequestsHandler(interfaceName, ip, port);
    }
    requestsHandler.notify(filteredUrls);

    /**
     * 对服务连接数default.connections进行处理
     */
    if (connectionsHandler == null) {
      connectionsHandler = new ConnectionsHandler(interfaceName, ip, port);
    }
    connectionsHandler.notify(filteredUrls);

    /**
     * 对服务是否过期deprecated进行处理
     */
    if (deprecatedHandler == null) {
      deprecatedHandler = new DeprecatedHandler(interfaceName, ip, port);
    }
    deprecatedHandler.notify(filteredUrls);

    /**
     * 对服务是否处于访问保护状态进行处理
     */
    if (accessProtectedHandler == null) {
      accessProtectedHandler = new AccessProtectedHandler(interfaceName, ip, port);
    }
    accessProtectedHandler.notify(filteredUrls);
  }

  /**
   * 过滤URL
   *
   * @author sxp
   * @since V1.0 2017-4-5
   */
  private List<URL> filterUrls(List<URL> urls) {
    Map<String, String> parameters;
    String protocol, ipOfUrl, application;

    List<URL> filteredUrls = new ArrayList<URL>(urls.size());

    for (URL url : urls) {
      if (url == null) {
        continue;
      }

      /**
       * 向注册中心订阅监听器成功后，监听节点下没有子节点，会立即返回一个协议为empty的URL <br>
       * 没有子节点有两种情况：一种是本来就没有，另一种是子节点被删除了
       */
      if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
        // 对于节点删除的情况也要进行处理
      } else {
        logger.debug("服务提供者[" + this.interfaceName + "]监听到服务配置信息: ["
                + url.toFullString() + "]");

        // 目前只对override操作做监听
        protocol = url.getProtocol();
        if (!RegistryConstants.OVERRIDE_PROTOCOL.equals(protocol)) {
          continue;
        }

        // 配置信息如果不是针对当前主机就不需要处理
        ipOfUrl = url.getIp();
        if (!RegistryConstants.ANYHOST_VALUE.equals(ipOfUrl) && !this.ip.equals(ipOfUrl)) {
          continue;
        }

        // 增加对application的过滤
        parameters = url.getParameters();
        if (parameters.containsKey(GlobalConstants.CommonKey.APPLICATION)) {
          application = parameters.get(GlobalConstants.CommonKey.APPLICATION);
        } else {
          application = null;
        }
        if (!StringUtils.isEmpty(application) && !application.equals(this.application)) {
          continue;
        }
      }

      filteredUrls.add(url);
    }

    return filteredUrls;
  }

  /**
   * 对URL按照一定的规则进行排序
   * <p>
   * 将优先级高的数据排在后面，升序排列。
   * </p>
   *
   * @author sxp
   * @since V1.0 2017-4-5
   * @since V1.1 2017-4-17 modify by sxp 明确的IP，优先级高于0.0.0.0
   */
  private List<URL> sort(List<URL> filteredUrls) {
    Collections.sort(filteredUrls, new UrlIpComparator());
    return filteredUrls;
  }
}
