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

import com.orientsec.grpc.consumer.core.ConsumerServiceRegistry;

public class AbstractListener {
  ConsumerServiceRegistry registry;
  ZookeeperNameResolver zookeeperNameResolver;

  AbstractListener(){
  }
  AbstractListener(ZookeeperNameResolver zookeeperNameResolver){
    this.zookeeperNameResolver = zookeeperNameResolver;
  }

  public void init(ZookeeperNameResolver zookeeperNameResolver,
                   ConsumerServiceRegistry registry){
    this.zookeeperNameResolver = zookeeperNameResolver;
    this.registry = registry;
  }

  public ConsumerServiceRegistry getRegistry() {
    return registry;
  }

  public void setRegistry(ConsumerServiceRegistry registry) {
    this.registry = registry;
  }

  public ZookeeperNameResolver getZookeeperNameResolver() {
    return zookeeperNameResolver;
  }

  public void setZookeeperNameResolver(ZookeeperNameResolver zookeeperNameResolver) {
    this.zookeeperNameResolver = zookeeperNameResolver;
  }
}
