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
package com.orientsec.grpc.consumer.check;

import com.orientsec.grpc.common.util.DateUtils;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 校验服务是否过时，如果过时则记录日志信息
 *
 * @author sxp
 * @since V1.0 2017/4/13
 */
public class CheckDeprecatedService {
  private static final Logger logger = LoggerFactory.getLogger(CheckDeprecatedService.class);

  /**
   * key值为服务接口名称，value值为该接口上一次记录deprecated日志的时间戳(此处不需要严格地控制并发)
   */
  private static Map<String, Long> lastLogDeprecatedTimes = new HashMap<String, Long>();

  /**
   * 校验
   *
   * @param serviceProviderMap 经过黑白名单、负载均衡策略过滤后，此处的serviceProviderMap应只有一条数据
   * @author sxp
   * @since V1.0
   */
  public static void check(Map<String, ServiceProvider> serviceProviderMap) {
    if (serviceProviderMap == null || serviceProviderMap.size() == 0) {
      return;
    }

    ServiceProvider provider = null;
    for (Map.Entry<String, ServiceProvider> entry : serviceProviderMap.entrySet()) {
      provider = entry.getValue();
      break;
    }

    if (provider != null) {
      boolean deprecated = provider.isDeprecated();

      if (deprecated) {
        String interfaceName = provider.getInterfaceName();

        long lastLogTime;// 上一次记录deprecated日志的时间戳

        if (!lastLogDeprecatedTimes.containsKey(interfaceName)) {
          lastLogTime = 0L;
          lastLogDeprecatedTimes.put(interfaceName, lastLogTime);
        } else {
          lastLogTime = lastLogDeprecatedTimes.get(interfaceName);
        }

        // 1天只打印一条日志
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime >= DateUtils.DAY_IN_MILLIS) {
          String msg = "服务[" + interfaceName + "]已经过时，请检查是否新服务上线替代了该服务";
          logger.warn(msg);

          lastLogDeprecatedTimes.put(interfaceName, currentTime);// 更新一下记录日志时间
        }
      }
    }
  }
}
