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


package com.orientsec.grpc.consumer.routers;

import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.exception.RpcException;

import java.util.Map;

public interface Router extends Comparable<Router> {
  /**
   * 获取路由规则的URL表示形式
   */
  URL getUrl();

  /**
   * 应用路由规则
   *
   * @param providers 客户端对应的所有服务端列表
   * @param url 客户端对应的URL
   * @return 应用路由规则后符合条件的服务端列表
   */
   Map<String,ServiceProvider> route(Map<String, ServiceProvider> providers, URL url) throws RpcException;
}
