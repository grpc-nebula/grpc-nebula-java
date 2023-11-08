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
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.resource.SystemConfig;
import com.orientsec.grpc.common.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一致性Hash参数
 * <p>
 * 存储、获取一致性Hash算法相关的数据
 * </p>
 *
 * @author sxp
 * @since 2018/10/19
 */
public class ConsistentHashArguments {
  /**
   * 各服务对应的参数列表的值
   * <p>
   * key值：服务名称 <br>
   * value值：参数列表转化为String拼接起来的字符串 <br>
   * </p>
   */
  private volatile static ThreadLocal<ConcurrentHashMap<String, Object>> arguments
          = new ThreadLocal<ConcurrentHashMap<String, Object>>() {
    @Override
    protected ConcurrentHashMap<String, Object> initialValue() {
      return new ConcurrentHashMap<String, Object>();
    }
  };

  /**
   * 当参数列表的大小达到多少时，开始回收value为空值的数据
   */
  private static final int RECYCLE_NULL_VALUE_THRESHOLD = 100;

  public static final String NULL_VALUE = "${ConsistentHashArguments-NULL}";

  /**
   * 本地配置文件中参数列表
   */
  private static Map<String, Boolean> validArgs;

  static {
    initValidArgs();
  }

  private static void initValidArgs() {
    Properties prop = SystemConfig.getProperties();
    if (prop == null) {
      validArgs = new HashMap<>(0);
      return;
    }

    String argumentList = prop.getProperty(GlobalConstants.Consumer.Key.HASH_ARGUMENTS);
    if (StringUtils.isEmpty(argumentList)) {
      validArgs = new HashMap<>(0);
      return;
    }

    String[] args = argumentList.split(",");

    validArgs = new HashMap<>(args.length);

    for (String arg : args) {
      if (!StringUtils.isEmpty(arg)) {
        arg = arg.trim();
        if (arg.length() > 0) {
          validArgs.put(arg, Boolean.TRUE);
        }
      }
    }
  }

  /**
   * 设置指定服务的参数列表对应的值
   *
   * @author sxp
   * @since 2018/10/19
   */
  public static void setArgument(String serviceName, Object value) {
    Preconditions.checkNotNull(serviceName, "serviceName");
    Preconditions.checkNotNull(value, "value");

    ConcurrentHashMap<String, Object> args = arguments.get();

    args.put(serviceName, value);
  }

  /**
   * 将指定服务的参数列表值设置为字符串"null"
   *
   * @author sxp
   * @since 2018/10/19
   */
  public static void resetArgument(String serviceName) {
    if (StringUtils.isEmpty(serviceName)) {
      return;
    }

    ConcurrentHashMap<String, Object> args = arguments.get();
    if (args.containsKey(serviceName)) {
      args.put(serviceName, NULL_VALUE);// ConcurrentHashMap的key和value至不能为null
    }

    if (args.size() >= RECYCLE_NULL_VALUE_THRESHOLD) {
      recycleNullValue();
    }
  }

  // 回收value为空值的数据
  private static void recycleNullValue() {
    ConcurrentHashMap<String, Object> args = arguments.get();

    List<String> invalidKeys = new ArrayList<String>();

    for (Map.Entry<String, Object> entry : args.entrySet()) {
      if (NULL_VALUE.equals(entry.getValue())) {
        invalidKeys.add(entry.getKey());
      }
    }

    if (invalidKeys.size() > 0) {
      for (String key : invalidKeys) {
        args.remove(key, NULL_VALUE);
      }
    }
  }


  /**
   * 获取指定服务的参数列表对应的值
   *
   * @author sxp
   * @since 2018/10/19
   */
  public static Object getArgument(String serviceName) {
    if (StringUtils.isEmpty(serviceName)) {
      return null;
    }

    return arguments.get().get(serviceName);
  }

  public static Map<String, Boolean> getValidArgs() {
    return validArgs;
  }
}
