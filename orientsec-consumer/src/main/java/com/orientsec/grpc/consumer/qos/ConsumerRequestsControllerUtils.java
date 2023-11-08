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
package com.orientsec.grpc.consumer.qos;

import com.orientsec.grpc.common.util.GrpcUtils;
import com.orientsec.grpc.common.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户端请求数控制器工具类
 *
 * @author sxp
 * @since V1.0 2017/3/29
 */
public class ConsumerRequestsControllerUtils {
  /**
   * 客户端请求数控制器的数据集
   * <p>
   * key值为服务接口名，即interface
   * </p>
   */
  private volatile static ConcurrentHashMap<String, Long> maxRequestsMap
          = new ConcurrentHashMap<>();

  /**
   * 上一次服务调用的时间，当前时间和上次时间超过1秒会更新数据
   */
  private volatile static ConcurrentHashMap<String, Long> lastInvokeTime
          = new ConcurrentHashMap<>();

  /**
   * 记录各服务1秒钟之内发起服务调用请求数，当前时间和上次时间超过1秒重新计数
   */
  private volatile static ConcurrentHashMap<String, AtomicLong> requestNumCounters
          = new ConcurrentHashMap<>();


  /**
   * 增加请求数
   */
  public static void addRequestNum(String fullMethodName) {
    String serviceName = GrpcUtils.getInterfaceNameNoneException(fullMethodName);
    if (StringUtils.isEmpty(serviceName)) {
      return;
    }

    //当前指定的服务不需要进行流控
    if (!maxRequestsMap.containsKey(serviceName)) {
      return;
    }

    long maxRequests = maxRequestsMap.get(serviceName).longValue();

    if (maxRequests <= 0) {
      return;
    }

    AtomicLong currentRequestNum;
    long currentTime = System.currentTimeMillis();

    if (requestNumCounters.containsKey(serviceName)) {
      currentRequestNum = requestNumCounters.get(serviceName);

      Long lastTimeObject = lastInvokeTime.get(serviceName);
      if (lastTimeObject == null) {
        lastTimeObject = 0L;
      }
      long lastTime = lastTimeObject.longValue();

      if(currentTime - lastTime > 1000) {
        lastInvokeTime.put(serviceName, currentTime);// 超过1秒时间更新
        currentRequestNum.set(0);// 超过1秒重新计数
      }
    } else {
      lastInvokeTime.put(serviceName, currentTime);

      currentRequestNum = new AtomicLong(0);
      AtomicLong oldValue = requestNumCounters.putIfAbsent(serviceName, currentRequestNum);
      if (oldValue != null) {
        currentRequestNum = oldValue;
      }
    }

    if (currentRequestNum.get() >= maxRequests) {
      throw new RuntimeException("当前客户端调用服务[" + serviceName + "]的请求数超出限制值[" + maxRequests + "]，请求失败！");
    }

    currentRequestNum.incrementAndGet();
  }

  /**
   * 设置调用某服务的最大请求数
   */
  public static void setMaxRequestsMap(String key, long maxRequests) {
    if (StringUtils.isEmpty(key)) {
      return;
    }
    if (maxRequestsMap.containsKey(key)) {
      if (maxRequests <= 0) {
        maxRequestsMap.remove(key);
      } else {
        maxRequestsMap.put(key, maxRequests);
      }
    } else if (maxRequests > 0) {
      //只有设置了最大请求数才需要控制
      maxRequestsMap.put(key, maxRequests);
    }
  }

  /**
   * 获得调用某服务的最大请求数
   *
   * @author sxp
   * @since 2019/1/31
   */
  public static long getMaxRequestsMap(String key) {
    if (StringUtils.isEmpty(key)) {
      return 0L;
    }

    if (maxRequestsMap.containsKey(key)) {
      Long maxRequests = maxRequestsMap.get(key);
      if (maxRequests == null) {
        maxRequests = 0L;
      }
      return maxRequests;
    }

    return 0L;
  }

  /**
   * 判断是否需要流控
   */
  public static boolean isNeedRequestsControl(String fullMethodName) {
    if (maxRequestsMap.size() == 0) {
      return false;
    }

    String serviceName = GrpcUtils.getInterfaceNameNoneException(fullMethodName);
    if (StringUtils.isEmpty(serviceName)) {
      return false;
    }

    //当前指定的服务不需要进行流控
    if (!maxRequestsMap.containsKey(serviceName)) {
      return false;
    }

    return true;
  }

}
