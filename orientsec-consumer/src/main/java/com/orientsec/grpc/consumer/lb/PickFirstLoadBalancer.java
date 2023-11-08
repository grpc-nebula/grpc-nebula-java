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
 * 随机选一个
 *
 * @author bona
 * @since 2018-04-13 13:36
 * @since 2018-4-20 modify by sxp 排序后再随机选一个
 */
public class PickFirstLoadBalancer {

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

    Set<String> keySet = serviceProviderMap.keySet();
    int serviceCount = keySet.size();

    ArrayList<String> keyList = new ArrayList<String>();
    keyList.addAll(keySet);
    Collections.sort(keyList);// 排序

    Random random = new Random();
    int index =  random.nextInt(serviceCount);
    String serverKey = keyList.get(index);

    Map<String, ServiceProvider> result = new ConcurrentHashMap<String, ServiceProvider>();
    result.put(serverKey, serviceProviderMap.get(serverKey));
    return result;
  }
}
