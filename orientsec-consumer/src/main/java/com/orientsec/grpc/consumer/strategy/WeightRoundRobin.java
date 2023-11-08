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

import java.util.ArrayList;
import java.util.List;

/**
 * 含有服务名称和服务提供者列表的包装对象
 */
public class WeightRoundRobin {
  private List<Server> servers = new ArrayList<>();

  public void setServers(List<Server> servers) {
    this.servers = servers;
  }

  public List<Server> getServers() {
    return servers;
  }

  /**
   * 平滑的加权轮询算法
   *
   * <p>
   * 资料：
   * https://blog.csdn.net/gqtcgq/article/details/52076997
   * https://tenfy.cn/2018/11/12/smooth-weighted-round-robin/
   * <p/>
   */
  public Server getBestServer() {
    Server server;
    Server best = null;
    int total = 0;
    int size = servers.size();

    for (int i = 0; i < size; i++) {
      server = servers.get(i);

      server.setCurrentWeight(server.getCurrentWeight() + server.getWeight());
      total += server.getWeight();

      if (best == null || server.getCurrentWeight() > best.getCurrentWeight()) {
        best = server;
      }
    }

    if (best == null) {
      return null;
    }

    best.setCurrentWeight(best.getCurrentWeight() - total);
    return best;
  }
}
