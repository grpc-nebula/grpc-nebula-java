/*
 * Copyright 2018-2019 The Apache Software Foundation
 * Modifications 2019 Orient Securities Co., Ltd.
 * Modifications 2019 BoCloud Inc.
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


package com.orientsec.grpc.registry;

import com.orientsec.grpc.registry.common.URL;

import java.util.List;

/**
 * Created by heiden on 2017/3/15.
 */
public interface RegistryService {
  /**
   * 注册数据，比如：提供者地址，消费者地址，路由规则，覆盖规则，等数据。
   * 注册需处理契约：<br>
   * 1. 当URL设置了check=false时，注册失败后不报错，在后台定时重试，否则抛出异常。<br>
   * 2. 当URL设置了dynamic=false参数，则需持久存储，否则，当注册者出现断电等情况异常退出时，需自动删除。<br>
   * 3. 当URL设置了category=routers时，表示分类存储，缺省类别为providers，可按分类部分通知数据。<br>
   * 4. 当注册中心重启，网络抖动，不能丢失数据，包括断线自动删除数据。<br>
   * 5. 允许URI相同但参数不同的URL并存，不能覆盖。<br>
   *
   * @param url 注册信息，不允许为空，如：
   *            grpc://192.168.1.211/com.orientsec.grpc.BarService?version=1.0.0&application=test
   */
  void register(URL url);

  /**
   * 取消注册.
   * 取消注册需处理契约：<br>
   * 1. 如果是dynamic=false的持久存储数据，找不到注册数据，则抛IllegalStateException，否则忽略。<br>
   * 2. 按全URL匹配取消注册。<br>
   *
   * @param url 注册信息，不允许为空，如：
   *            grpc://192.168.1.211/com.orientsec.grpc.BarService?version=1.0.0&application=test
   */
  void unregister(URL url);

  /**
   * 订阅符合条件的已注册数据，当有注册数据变更时自动推送.
   * 订阅需处理契约：<br>
   * 1. 当URL设置了check=false时，订阅失败后不报错，在后台定时重试。<br>
   * 2. 当URL设置了category=routers，只通知指定分类的数据，多个分类用逗号分隔，
   *    并允许星号通配，表示订阅所有分类数据。<br>
   * 3. 允许以interface,group,version,classifier作为条件查询，如：
   *    interface=com.alibaba.foo.BarService&version=1.0.0<br>
   * 4. 并且查询条件允许星号通配，订阅所有接口的所有分组的所有版本，
   *    或：interface=*&group=*&version=*&classifier=*<br>
   * 5. 当注册中心重启，网络抖动，需自动恢复订阅请求。<br>
   * 6. 允许URI相同但参数不同的URL并存，不能覆盖。<br>
   * 7. 必须阻塞订阅过程，等第一次通知完后再返回。<br>
   *
   * @param url      订阅条件，不允许为空，如：
   *     consumer://192.168.1.211/com.orientsec.grpc.BarService?version=1.0.0&application=test
   * @param listener 变更事件监听器，不允许为空
   */
  void subscribe(URL url, NotifyListener listener);

  /**
   * 取消订阅.
   * 取消订阅需处理契约：<br>
   * 1. 如果没有订阅，直接忽略。<br>
   * 2. 按全URL匹配取消订阅。<br>
   *
   * @param url      订阅条件，不允许为空，如：
   *      consumer://192.168.1.211/com.orientsec.grpc.BarService?version=1.0.0&application=test
   * @param listener 变更事件监听器，不允许为空
   */
  void unsubscribe(URL url, NotifyListener listener);



  /**
   * 查询符合条件的已注册数据，与订阅的推模式相对应，这里为拉模式，只返回一次结果.<br>
   *
   * @param url 查询条件，不允许为空，如：
   *      consumer://192.168.1.211/com.orientsec.grpc.BarService?version=1.0.0&application=test
   * @return 已注册信息列表，可能为空，含义同
   *
   */
  List<URL> lookup(URL url);

  /**
   * 提取指定目录下的数据
   * @param path  zk节点下的路径
   * @return 数据
   */
  String getData(String path);
}
