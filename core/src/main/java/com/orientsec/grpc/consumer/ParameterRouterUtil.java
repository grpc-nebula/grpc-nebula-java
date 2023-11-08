/*
 * Copyright 2020 Orient Securities Co., Ltd.
 * Copyright 2020 BoCloud Inc.
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

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.runtime.type.AviatorFunction;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.resource.SystemConfig;
import com.orientsec.grpc.common.util.PropertiesUtils;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.consumer.routers.ParameterRouter;
import com.orientsec.grpc.registry.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 参数路由相关工具类
 *
 * @since nebula-1.2.8 2020-12-07
 * @author zhuyujie
 */
public class ParameterRouterUtil {

  private static final Logger logger = LoggerFactory.getLogger(ParameterRouterUtil.class);

  private static final Properties properties = SystemConfig.getProperties();

  private static final boolean enabled = initParameterRouterEnabled();

  /**
   * 保存AviatorEvaluatorInstance的Map，每个service创建一个instance，用来保存缓存的Expression.
   * key:serviceName, value:AviatorEvaluatorInstance
   */
  private static final ConcurrentHashMap<String, AviatorEvaluatorInstance> aviatorInstanceMap = new ConcurrentHashMap<>();

  /**
   * 保存用户创建的自定义Function
   * key:functionName, value:AviatorFunction
   */
  private static final ConcurrentHashMap<String, AviatorFunction> userFunctionMap = new ConcurrentHashMap<>();

  private static boolean initParameterRouterEnabled() {
    String key = GlobalConstants.CommonKey.PARAMETER_ROUTER_ENABLED;
    return PropertiesUtils.getValidBooleanValue(properties, key, true);
  }

  public static boolean isEnabled() {
    return enabled;
  }

  /**
   * 根据所传参数和配置的规则来过滤服务端列表
   *
   * @since nebula-1.2.8 2020-12-07
   * @author zhuyujie
   */
  public static Map<String, ServiceProvider> filterByParameterAndRule(Map<String, ServiceProvider> serviceProviderMap,
                                                                      List<ParameterRouter> parameterRouterList,
                                                                      Map<String, Object> parameterMap,
                                                                      URL consumerUrl) {
    Map<String, ServiceProvider> newProviderMap = new HashMap<>(serviceProviderMap);
    for (ParameterRouter router : parameterRouterList) {
      newProviderMap = router.route(newProviderMap, consumerUrl, parameterMap);
      if (newProviderMap.size() == 0) {
        break;
      }
    }
    return newProviderMap;
  }

  /**
   * 按照服务名获取Aviator实例，若不存在就创建
   *
   * @since nebula-1.2.8 2020-12-07
   * @author zhuyujie
   */
  public static AviatorEvaluatorInstance getAviatorInstance(String serviceName) {
    if (aviatorInstanceMap.containsKey(serviceName)) {
      return aviatorInstanceMap.get(serviceName);
    }

    AviatorEvaluatorInstance instance = AviatorEvaluator.newInstance();
    for (Map.Entry<String, AviatorFunction> entry : userFunctionMap.entrySet()) {
      instance.addFunction(entry.getValue());
    }
    aviatorInstanceMap.putIfAbsent(serviceName, instance);
    return aviatorInstanceMap.get(serviceName);
  }

  /**
   * 按照服务名向Aviator实例添加自定义函数
   *
   * @since nebula-1.2.8 2020-12-07
   * @author zhuyujie
   */
  public static void addFunction(String serviceName, AviatorFunction function) {
    if (!aviatorInstanceMap.containsKey(serviceName)) {
      throw new RuntimeException("服务名称不存在或尚未创建该服务的AviatorEvaluatorInstance实例！");
    }

    getAviatorInstance(serviceName).addFunction(function);
  }

  /**
   * 按照服务名向Aviator实例删除自定义函数
   *
   * @since nebula-1.2.8 2020-12-07
   * @author zhuyujie
   */
  public static void removeFunction(String serviceName, String functionName) {
    if (!aviatorInstanceMap.containsKey(serviceName)) {
      throw new RuntimeException("服务名称不存在或尚未创建该服务的AviatorEvaluatorInstance实例！");
    }

    getAviatorInstance(serviceName).removeFunction(functionName);
  }

  /**
   * 全局添加自定义函数
   *
   * @since nebula-1.2.8 2020-12-07
   * @author zhuyujie
   */
  public static void addGlobalFunction(AviatorFunction function) {
    userFunctionMap.put(function.getName(), function);
    for (Map.Entry<String, AviatorEvaluatorInstance> entry : aviatorInstanceMap.entrySet()) {
      entry.getValue().addFunction(function);
    }
  }

  /**
   * 全局删除自定义函数
   *
   * @since nebula-1.2.8 2020-12-07
   * @author zhuyujie
   */
  public static void removeGlobalFunction(String functionName) {
    userFunctionMap.remove(functionName);
    for (Map.Entry<String, AviatorEvaluatorInstance> entry : aviatorInstanceMap.entrySet()) {
      entry.getValue().removeFunction(functionName);
    }
  }

}
