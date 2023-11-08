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
package com.orientsec.grpc.consumer.task;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.exception.BusinessException;
import com.orientsec.grpc.consumer.core.DefaultConsumerServiceRegistryImpl;
import com.orientsec.grpc.registry.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * 查询
 * <p>
 * 根据参数获取服务provider列表信息
 * </p>
 *
 * @author dengjq
 * @since V1.0 2017/3/31
 */
public class LookupTask extends AbstractTask {

  private static final Logger logger = LoggerFactory.getLogger(LookupTask.class);

  private Map<String, String> providerParams = null;

  public LookupTask(DefaultConsumerServiceRegistryImpl caller, Map<String, String> consumersParams) {
    super(caller);
    this.providerParams = consumersParams;
  }

  /**
   * 主方法
   *
   * @author djq
   * @since V1.0 2017/3/31
   */
  public List<URL> work() throws Exception {
    if (providerParams == null) {
      logger.error("注册服务失败:传入的参数servicesParams为空");
      throw new BusinessException("注册服务失败:传入的参数servicesParams为空");
    }
    if (!providerParams.containsKey(GlobalConstants.Provider.Key.INTERFACE)) {
      logger.info("查询providers时必须提供interface参数");
      return null;
    }
    String value, valueOfS;
    Map<String, String> parameters = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : providerParams.entrySet()) {
      value = entry.getValue();
      valueOfS = (value == null) ? (null) : String.valueOf(value);
      if (valueOfS != null) {
        parameters.put(entry.getKey(), valueOfS);
      }
    }
    parameters.put(RegistryConstants.CATEGORY_KEY, RegistryConstants.PROVIDERS_CATEGORY);
    URL url = new URL(RegistryConstants.GRPC_PROTOCOL, caller.getIp(), 0, parameters);
    return safeLookup(url);
  }
}
