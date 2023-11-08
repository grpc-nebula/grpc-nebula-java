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

package com.orientsec.grpc.consumer.routers;

import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Expression;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.consumer.ParameterRouterUtil;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 参数路由实体类，根据传入的参数匹配对应的规则。
 * <p>规则支持的运算包括但不限于： </p>
 * <p>1. 等于== 示例：owner == 'zhuyujie' </p>
 * <p>2. 大于>、小于< 示例：cost_id > 1000 </p>
 * <p>3. 取反! 示例：arg0 != 'abc' </p>
 * <p>4. 且&&、或|| 示例：userId == 'A0001' && method != 'getData' </p>
 * <p>5. 包含于 示例：include(seq.list('A0001', 'A0002', 'A0003'), userId) </p>
 * <p>6. 其他内置函数和自定义函数 </p>
 *
 * @since nebula-1.2.8 2020-12-07
 * @author zhuyujie
 */
public class ParameterRouter implements Router {

  private static final Logger logger = LoggerFactory.getLogger(ParameterRouter.class);

  private static final Boolean EXPRESSION_CACHED = true;

  private static final String DEFAULT_MESSAGE = "匹配到参数路由规则过滤服务提供者。规则提示：";

  private final URL url;
  private final String rule;
  private final String target;
  private final int priority;
  private final boolean enabled;
  private final String message;

  private final Expression expression;

  public ParameterRouter(URL url) {
    this.url = url;
    this.priority = url.getParameter(Constants.PRIORITY_KEY, 0);
    this.enabled = url.getParameter(Constants.ENABLED_KEY, true);

    String fullRule = url.getParameterAndDecoded(Constants.RULE_KEY);
    this.message = DEFAULT_MESSAGE + "[" + url.getParameter(Constants.MESSAGE, "") + "]";
    if (fullRule == null || fullRule.trim().length() == 0) {
      throw new IllegalArgumentException("Illegal parameter route rule!");
    }
    String[] ruleAndTarget = fullRule.split("=>");
    if (ruleAndTarget.length > 2 || ruleAndTarget.length < 1) {
      throw new IllegalArgumentException("Illegal parameter route rule!");
    }
    this.rule = ruleAndTarget[0].trim();
    this.target = ruleAndTarget.length == 2 ? ruleAndTarget[1].trim() : null;

    AviatorEvaluatorInstance aviatorEvaluatorInstance = ParameterRouterUtil.getAviatorInstance(url.getServiceInterface());
    expression = aviatorEvaluatorInstance.compile(rule, EXPRESSION_CACHED);
  }

  @Override
  public URL getUrl() {
    return this.url;
  }

  @Override
  public Map<String, ServiceProvider> route(Map<String, ServiceProvider> providers, URL url) throws RpcException {
    return this.route(providers, url, null);
  }

  public Map<String, ServiceProvider> route(Map<String, ServiceProvider> providers, URL url,
                                            Map<String, Object> parameterMap) throws RpcException {
    if (!enabled) {
      // 如果该条参数路由未被启用，则跳过这条参数路由
      return providers;
    }

    Map<String, Object> allParamMap = parameterMap == null ? new HashMap<String, Object>() : new HashMap<>(parameterMap);
    allParamMap.putAll(url.toMap());
    allParamMap.remove("port");

    boolean matched = (boolean) expression.execute(allParamMap);
    if (!matched) {
      // 如果规则与参数不匹配，则跳过这条参数路由
      return providers;
    }

    logger.info(message);
    if (StringUtils.isEmpty(target)) {
      // 如果规则与参数匹配且target为空，则不允许其访问任何服务端实例
      return Collections.emptyMap();
    }

    // 如果规则与参数匹配且target不为空，则排除所有target之外的服务端实例
    String[] addressList = target.split(",");
    Set<String> addressSet = new HashSet<>();
    for (String address : addressList) {
      addressSet.add(address.trim());
    }

    Map<String, ServiceProvider> newProviders = new HashMap<>();
    for (Map.Entry<String, ServiceProvider> entry : providers.entrySet()) {
      ServiceProvider provider = entry.getValue();
      String host = provider.getHost();
      String address = host + ":" + provider.getPort();
      if (addressSet.contains(host) || addressSet.contains(address)) {
        newProviders.put(entry.getKey(), entry.getValue());
      }
    }
    return newProviders;
  }

  public int getPriority() {
    return priority;
  }

  @Override
  public int compareTo(Router o) {
    if (o == null || o.getClass() != ParameterRouter.class) {
      return 1;
    }
    ParameterRouter c = (ParameterRouter) o;
    return this.priority == c.priority ? url.toFullString().compareTo(c.url.toFullString()) : (this.priority > c.priority ? 1 : -1);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ParameterRouter)) return false;

    ParameterRouter router = (ParameterRouter) o;

    if (priority != router.priority) return false;
    if (url != null ? !url.equals(router.url) : router.url != null) return false;
    if (rule != null ? !rule.equals(router.rule) : router.rule != null) return false;
    return target != null ? target.equals(router.target) : router.target == null;
  }

  @Override
  public int hashCode() {
    int result = url != null ? url.hashCode() : 0;
    result = 31 * result + (rule != null ? rule.hashCode() : 0);
    result = 31 * result + (target != null ? target.hashCode() : 0);
    result = 31 * result + priority;
    return result;
  }
}
