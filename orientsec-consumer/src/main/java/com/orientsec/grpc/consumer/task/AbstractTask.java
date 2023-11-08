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
package com.orientsec.grpc.consumer.task;



import com.orientsec.grpc.consumer.core.DefaultConsumerServiceRegistryImpl;
import com.orientsec.grpc.consumer.watch.ConsumerListener;
import com.orientsec.grpc.registry.common.URL;

import java.util.List;

/**
 * 封装任务内，处理注册中心客户端初始化失败的情形
 */
public class AbstractTask {

  DefaultConsumerServiceRegistryImpl caller;

  public AbstractTask(DefaultConsumerServiceRegistryImpl caller) {
    this.caller = caller;
  }

  /**
   * 安全注册
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void safeRegistry(URL url) {
    if (caller.isConsumerInit()) {
      caller.getConsumer().registerService(url);
    }
  }

  /**
   * 安全注销
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void safeUnRegistry(URL url) {
    if (caller.isConsumerInit()) {
      caller.getConsumer().unRegisterService(url);
    }
  }

  public void safeReleaseRegistry() {
    if (caller.isConsumerInit()) {
      caller.getConsumer().releaseRegistry();
    }
  }

  /**
   * 安全订阅
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void safeSubscribe(URL url, ConsumerListener listener) {
    if (caller.isConsumerInit()) {
      caller.getConsumer().subscribe(url, listener);
    }
  }

  /**
   * 安全取消订阅
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void safeUnSubscribe(URL url, ConsumerListener listener) {
    if (caller.isConsumerInit()) {
      caller.getConsumer().unSubscribe(url, listener);
    }
  }

  /**
   * 安全查询
   *
   * @author sxp
   * @since 2018/12/1
   */
  public List<URL> safeLookup(URL url) {
    if (caller.isConsumerInit()) {
      return caller.getConsumer().lookup(url);
    }
    return null;
  }
}
