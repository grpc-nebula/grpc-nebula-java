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
package com.orientsec.grpc.provider.qos;

import com.google.common.base.Preconditions;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.exception.BusinessException;
import com.orientsec.grpc.common.resource.SystemSwitch;
import com.orientsec.grpc.common.util.MathUtils;
import com.orientsec.grpc.provider.core.ServiceConfigUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务连接数控制器工具类
 *
 * @author sxp
 * @since V1.0 2017/3/29
 */
public class ProviderRequestsControllerUtils {
  /**
   * 服务连接数控制器的数据集
   * <p>
   * key值为服务接口名，即interface
   * </p>
   */
  private volatile static ConcurrentHashMap<String, RequestsController> controllers = new ConcurrentHashMap<>();


  /**
   * 检验当前服务接口的连接数是否满足条件
   * <p>
   * 即当前接口的连接数是否已经达到允许的最大值
   * </p>
   *
   * @author sxp
   * @since V1.0 2017-3-29
   */
  public static boolean checkRequests(String interfaceName) {
    if (!SystemSwitch.PROVIDER_ENABLED) {
      return true;
    }

    Preconditions.checkNotNull(interfaceName, "interfaceName");

    RequestsController controller;
    if (!controllers.containsKey(interfaceName)) {
      controller = getControllerInstance(interfaceName);
      RequestsController oldValue = controllers.putIfAbsent(interfaceName, controller);

      if (oldValue != null) {
        // 防止其他线程在这段时间内已经向controllers中写入了新数据
        controller = oldValue;
      }
    } else {
      controller = controllers.get(interfaceName);
    }

    return controller.checkRequests();
  }

  /**
   * 获取服务接口的最大请求数
   *
   * @author sxp
   * @since V1.0 2017-4-1
   */
  public static int getMaxRequests(String interfaceName) {
    if (!SystemSwitch.PROVIDER_ENABLED) {
      return RequestsController.NO_LIMIT_NUM;
    }

    Preconditions.checkNotNull(interfaceName, "interfaceName");

    RequestsController controller;
    if (!controllers.containsKey(interfaceName)) {
      controller = getControllerInstance(interfaceName);
      RequestsController oldValue = controllers.putIfAbsent(interfaceName, controller);

      if (oldValue != null) {
        // 防止其他线程在这段时间内已经向controllers中写入了新数据
        controller = oldValue;
      }
    } else {
      controller = controllers.get(interfaceName);
    }

    return controller.getMax();
  }

  /**
   * 获取服务接口的当前请求数
   *
   * @author sxp
   * @since 2018-6-6
   */
  public static int getCurrentRequests(String interfaceName) {
    if (!SystemSwitch.PROVIDER_ENABLED) {
      return 0;
    }

    Preconditions.checkNotNull(interfaceName, "interfaceName");

    RequestsController controller;
    if (!controllers.containsKey(interfaceName)) {
      return 0;
    } else {
      controller = controllers.get(interfaceName);
      return controller.getCurrent();
    }
  }

  /**
   * 将指定服务的当前连接数加1
   *
   * @author sxp
   * @since V1.0 2017-3-29
   */
  public static boolean increaseRequest(String interfaceName) {
    if (!SystemSwitch.PROVIDER_ENABLED) {
      return true;
    }

    Preconditions.checkNotNull(interfaceName, "interfaceName");

    RequestsController controller;

    if (controllers.containsKey(interfaceName)) {
      controller = controllers.get(interfaceName);
    } else {
      controller = getControllerInstance(interfaceName);
      RequestsController oldValue = controllers.putIfAbsent(interfaceName, controller);

      if (oldValue != null) {
        // 防止其他线程在调用这段时间内已经向controllers中写入了新数据
        controller = oldValue;
      }
    }

    return controller.increase();
  }

  /**
   * 将指定服务的当前连接数减1
   *
   * @author sxp
   * @since V1.0 2017-3-29
   */
  public static void decreaseRequest(String interfaceName) {
    if (!SystemSwitch.PROVIDER_ENABLED) {
      return;
    }

    Preconditions.checkNotNull(interfaceName, "interfaceName");

    RequestsController controller;

    if (controllers.containsKey(interfaceName)) {
      controller = controllers.get(interfaceName);
    } else {
      controller = getControllerInstance(interfaceName);
      RequestsController oldValue = controllers.putIfAbsent(interfaceName, controller);

      if (oldValue != null) {
        // 防止其他线程在调用这段时间内已经向controllers中写入了新数据
        controller = oldValue;
      }
    }

    controller.decrease();
  }

  /**
   * 创建一个服务连接数控制器
   *
   * @author sxp
   * @since 2018/12/1
   */
  private static RequestsController getControllerInstance(String interfaceName) {
    Map<String, Map<String, Object>> currentServicesConfig = ServiceConfigUtils.getCurrentServicesConfig();

    if (!currentServicesConfig.containsKey(interfaceName)) {
      throw new BusinessException("未找到[" + interfaceName + "]对应的服务配置信息，请联系管理员解决！");
    }

    Map<String, Object> config = currentServicesConfig.get(interfaceName);
    String requests = (String) config.get(GlobalConstants.Provider.Key.DEFAULT_REQUESTS);

    int requestsNum = GlobalConstants.Provider.DEFAULT_REQUESTS_NUM;
    if (MathUtils.isInteger(requests)) {
      requestsNum = Integer.parseInt(requests);
      requestsNum = RequestsController.getValidMax(requestsNum);
    }

    RequestsController controller = new RequestsController(requestsNum);

    return controller;
  }

  /**
   * 获取请求数控制器的集合
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static ConcurrentHashMap<String, RequestsController> getControllers() {
    return controllers;
  }


}
