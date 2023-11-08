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

import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.consumer.routers.ConditionRouter;
import com.orientsec.grpc.consumer.routers.ParameterRouter;
import com.orientsec.grpc.consumer.routers.Router;
import com.orientsec.grpc.consumer.watch.ConsumerListener;
import com.orientsec.grpc.registry.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 路由规则监听器
 *
 * @author dengjianqian
 * @since 2018-8-10 modify by sxp 修正bug：当客户端处在黑名单中，启动客户端后再将黑名单拿掉，客户端还是不能连上服务端
 * @since 2019-7-25 modify by sxp 修正bug：有两个服务端AB的情况下，先将客户端设置为服务端A的黑名单，然后再删除黑名单，会出现客户端无法再调用服务端A
 */
public class RoutersListener extends  AbstractListener implements ConsumerListener {

  private static final Logger logger = LoggerFactory.getLogger(RoutersListener.class);

  private boolean initData;

  public RoutersListener(){
      initData = true;
  }

  @Override
  public void notify(List<URL> urls) {
    List<Router> routes = new ArrayList<Router>();
    List<ParameterRouter> parameterRouterList = new ArrayList<>();
    for (URL url : urls) {
      if (RegistryConstants.PARAMETER_ROUTER_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
        ParameterRouter router;
        try {
          router = new ParameterRouter(url);
        } catch (Exception e) {
          logger.info("监听到错误的参数路由表达式，自动跳过。url为[" + url + "]", e);
          continue;
        }
        parameterRouterList.add(router);
      } else if (RegistryConstants.ROUTER_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
        Router router = new ConditionRouter(url);
        routes.add(router);
      }
    }
    updateParameterRouterRule(parameterRouterList);

    // 仅有参数路由规则的更新不会触发服务端列表更新
    List<Router> lastRoutes = zookeeperNameResolver.getRoutes();
    if (lastRoutes.containsAll(routes) && routes.containsAll(lastRoutes)) {
      return;
    }

    Collections.sort(routes);
    zookeeperNameResolver.getRoutes().clear();
    zookeeperNameResolver.setRoutes(routes);

    // 只要路由规则发生变化，就需要更新服务端列表
    String serviceName = zookeeperNameResolver.getServiceName();
    Object lock = getZookeeperNameResolver().getLock();
    synchronized (lock) {
      zookeeperNameResolver.getAllByName(serviceName);
    }

    // 路由规则变化后，重置providersForLoadBalance
    getZookeeperNameResolver().setProvidersForLoadBalance(new ConcurrentHashMap<String, ServiceProvider>());
    getZookeeperNameResolver().setProvidersForLoadBalanceFlag(0);

    //第一次调用时(订阅时)不刷新providers缓存
    if (!initData){
      zookeeperNameResolver.resolveServerInfoWithLock();
    }
    initData = false;
  }

  private void updateParameterRouterRule(List<ParameterRouter> parameterRouterList) {
    List<ParameterRouter> routers = zookeeperNameResolver.getParameterRouters();
    routers.clear();
    Collections.sort(parameterRouterList);
    zookeeperNameResolver.setParameterRouters(parameterRouterList);
  }
}
