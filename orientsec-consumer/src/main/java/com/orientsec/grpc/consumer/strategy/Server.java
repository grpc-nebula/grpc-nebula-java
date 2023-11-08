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
package com.orientsec.grpc.consumer.strategy;

/**
 * 服务提供者实体
 * <p>
 * 用于基于权重的负载均衡算法
 * </p>
 *
 * @author Administrator
 * @since 2018/4/14
 */
public class Server {
  /**
   * 键值(ip:port)
   */
  private String key;

  /**
   * 权重值
   */
  private int weight;

  /**
   * 当前权重
   */
  private int currentWeight;


  /**
   * 获取权重
   *
   * @author sxp
   * @since 2018/12/1
   */
  public int getWeight() {
    return weight;
  }

  /**
   * 设置权重
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setWeight(int weight) {
    this.weight = weight;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Server(String key, int weight) {
    this.key = key;
    this.weight = weight;
    this.currentWeight = 0;
  }

  public int getCurrentWeight() {
    return currentWeight;
  }

  public void setCurrentWeight(int currentWeight) {
    this.currentWeight = currentWeight;
  }
}
