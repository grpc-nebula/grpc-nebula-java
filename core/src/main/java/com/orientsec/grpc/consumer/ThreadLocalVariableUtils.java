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
package com.orientsec.grpc.consumer;

import com.google.common.base.Preconditions;
import com.orientsec.grpc.common.util.GrpcUtils;
import com.orientsec.grpc.common.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程变量工具类
 *
 * @Author yuanzhonglin
 * @since 2019/4/15
 */
public class ThreadLocalVariableUtils {

  /**
   * 存放当前调用方法名的线程变量
   * <p>
   * key值为服务名，value值为方法名
   * <p/>
   */
  private volatile static ThreadLocal<ConcurrentHashMap<String, String>> serviceMethodNames
          = new ThreadLocal<ConcurrentHashMap<String, String>>() {
    @Override
    protected ConcurrentHashMap<String, String> initialValue() {
      return new ConcurrentHashMap<>();
    }
  };

  /**
   * 保存当前调用的方法名
   *
   * @param serviceName 当前调用的服务名
   * @param fullMethodName 当前调用的全路径方法名
   */
  public static void setServiceMethodName(String serviceName, String fullMethodName) {
    Preconditions.checkNotNull(serviceName, "serviceName");
    Preconditions.checkNotNull(fullMethodName, "fullMethodName");

    String method = GrpcUtils.getSimpleMethodName(fullMethodName);
    ConcurrentHashMap<String, String> names = serviceMethodNames.get();
    names.put(serviceName, method);
  }


  /**
   * 查询当前调用的方法名
   *
   * @param serviceName 当前调用的服务名
   */
  public static String getServiceMethodName(String serviceName) {
    if (StringUtils.isEmpty(serviceName)) {
      return null;
    }

    return serviceMethodNames.get().get(serviceName);
  }
}
