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
package com.orientsec.grpc.common.util;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.enums.LoadBalanceMode;
import io.grpc.NameResolver;

import java.util.Map;

/**
 * 负载均衡模式与策略获取
 *
 * @Author yuanzhonglin
 * @since 2019/4/16
 */
public class LoadBalanceUtil {
  public static final String EMPTY_METHOD = GlobalConstants.LOAD_BALANCE_EMPTY_METHOD;

  /**
   * 获取负载均衡模式
   *
   * @Author yuanzhonglin
   * @since 2019/4/16
   */
  public static String getLoadBalanceMode(NameResolver nameResolver, String method) {
    String mode = null;
    if (nameResolver != null && nameResolver.getManagedChannel() != null
            && nameResolver.getManagedChannel().getLoadBalanceModeMap() != null) {
      Map<String, String> modeMap = nameResolver.getManagedChannel().getLoadBalanceModeMap();
      if (StringUtils.isNotEmpty(method) && modeMap.containsKey(method)) {
        mode = modeMap.get(method);
      } else {
        mode = modeMap.get(EMPTY_METHOD);
      }
    }
    if (StringUtils.isEmpty(mode)) {
      mode = LoadBalanceMode.connection.name();
    }
    return mode;
  }

  /**
   * 获取负载均衡策略
   *
   * @Author yuanzhonglin
   * @since 2019/4/18
   * @since 2020/06/22 modify by wlh 修改默认负载均衡策略为随机
   */
  public static GlobalConstants.LB_STRATEGY getLoadBalanceStrategy(Map<String, GlobalConstants.LB_STRATEGY> map,
                                                                   String method) {
    GlobalConstants.LB_STRATEGY lb = null;
    if (map != null && map.containsKey(EMPTY_METHOD)) {
      if (StringUtils.isNotEmpty(method) && map.containsKey(method)) {
        lb = map.get(method);
      } else {
        lb = map.get(EMPTY_METHOD);
      }
    }
    if (lb == null) {
      lb = GlobalConstants.LB_STRATEGY.PICK_FIRST;
    }
    return lb;
  }

}
