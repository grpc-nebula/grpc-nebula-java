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
/*
 * Copyright 2015, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.orientsec.grpc.consumer.lb;

import com.orientsec.grpc.consumer.model.ServiceProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轮询
 *
 * @author yangzhenrong
 * @since 2018-04-13 13:37
 * @since 2018-5-25 modify by sxp 该场景不需要严格地控制并发，只需要在大体上体现出轮询的效果即可
 */
public class RoundRobinLoadBalancer {
  private static Map<String, Integer> indexMap = new ConcurrentHashMap<String, Integer>();

  /**
   * 选择服务提供者
   *
   * @param serviceProviderMap 服务提供者集合
   * @author sxp
   * @since 2018/12/1
   */
  public static Map<String, ServiceProvider> chooseProvider(Map<String, ServiceProvider> serviceProviderMap) {
    if (serviceProviderMap == null || serviceProviderMap.isEmpty() || serviceProviderMap.size() == 1) {
      return serviceProviderMap;
    }

    Map<String, ServiceProvider> result = new ConcurrentHashMap<String, ServiceProvider>();

    Set<String> keySet = serviceProviderMap.keySet();
    ArrayList<String> keyList = new ArrayList<String>();
    keyList.addAll(keySet);
    Collections.sort(keyList);

    int serviceCount = keySet.size();
    String interfaceName = getInterfaceName(serviceProviderMap);
    String serverKey;

    // 使用一个临时变量tempIndex避免keyList.get(tempIndex)时数组越界
    int tempIndex = getIndex(interfaceName, serviceCount) + 1;
    if (tempIndex >= serviceCount) {
      tempIndex = tempIndex % serviceCount;
    }
    serverKey = keyList.get(tempIndex);

    indexMap.put(interfaceName, tempIndex);

    result.put(serverKey, serviceProviderMap.get(serverKey));

    return result;
  }

  /**
   * 获取当前的下标值
   * <p>第一次取值总是使用随机数</p>
   *
   * @author sxp
   * @since 2018/12/1
   */
  private static Integer getIndex(String interfaceName, int serviceCount) {
    if (null == indexMap || !indexMap.containsKey(interfaceName)) {
      Random random = new Random();
      return random.nextInt(serviceCount);
    } else {
      return indexMap.get(interfaceName);
    }
  }

  /**
   * 获取接口名称
   *
   * @since 2019-2-1 modify by sxp 修正bug
   */
  static String getInterfaceName(Map<String, ServiceProvider> serviceProviderMap) {
    if (serviceProviderMap == null || serviceProviderMap.isEmpty()) {
      return null;
    }

    for (Map.Entry<String, ServiceProvider> entry : serviceProviderMap.entrySet()) {
      return entry.getValue().getInterfaceName();
    }

    return null;
  }
}
