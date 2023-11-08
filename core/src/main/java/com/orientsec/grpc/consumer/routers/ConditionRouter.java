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


package com.orientsec.grpc.consumer.routers;

import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.common.utils.NetUtils;
import com.orientsec.grpc.registry.common.utils.StringUtils;
import com.orientsec.grpc.registry.common.utils.UrlUtils;
import com.orientsec.grpc.registry.exception.RpcException;

import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionRouter implements Router, Comparable<Router> {

  private static final Logger logger = Logger.getLogger(ConditionRouter.class.getName());
  private static Pattern ROUTE_PATTERN = Pattern.compile("([&!=,]*)\\s*([^&!=,\\s]+)");
  private final URL url;
  private final int priority;
  private final boolean force;
  private final Map<String, MatchPair> whenCondition;
  private final Map<String, MatchPair> thenCondition;


  public ConditionRouter(URL url) {
    this.url = url;
    this.priority = url.getParameter(Constants.PRIORITY_KEY, 0);

    // 当路由结果为空时，是否强制执行，如果不强制执行，路由结果为空的路由规则将自动失效。缺省值为true。
    this.force = url.getParameter(Constants.FORCE_KEY, true);

    try {
      String rule = url.getParameterAndDecoded(Constants.RULE_KEY);
      if (rule == null || rule.trim().length() == 0) {
        throw new IllegalArgumentException("Illegal route rule!");
      }
      rule = rule.replace("consumer.", "").replace("provider.", "");
      int i = rule.indexOf("=>");
      String whenRule = i < 0 ? null : rule.substring(0, i).trim();
      String thenRule = i < 0 ? rule.trim() : rule.substring(i + 2).trim();
      Map<String, MatchPair> when = StringUtils.isBlank(whenRule) || "true".equals(whenRule) ? new HashMap<String, MatchPair>() : parseRule(whenRule);
      Map<String, MatchPair> then = StringUtils.isBlank(thenRule) || "false".equals(thenRule) ? null : parseRule(thenRule);
      // NOTE: When条件是允许为空的，外部业务来保证类似的约束条件
      this.whenCondition = when;
      this.thenCondition = then;
    } catch (ParseException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  private static Map<String, MatchPair> parseRule(String rule)
          throws ParseException {
    Map<String, MatchPair> condition = new HashMap<String, MatchPair>();
    if (StringUtils.isBlank(rule)) {
      return condition;
    }
    // 匹配或不匹配Key-Value对
    MatchPair pair = null;
    // 多个Value值
    Set<String> values = null;

    // 通过正则表达式匹配路由规则，ROUTE_PATTERN = ([&!=,]*)\s*([^&!=,\s]+)
    // 这个表达式看起来不是很好理解，第一个括号内的表达式用于匹配"&", "!", "=" 和 "," 等符号。
    // 第二括号内的用于匹配英文字母，数字等字符。举个例子说明一下：
    //    host = 2.2.2.2 & host != 1.1.1.1 & method = hello
    // 匹配结果如下：
    //     括号一      括号二
    // 1.  null       host
    // 2.   =         2.2.2.2
    // 3.   &         host
    // 4.   !=        1.1.1.1
    // 5.   &         method
    // 6.   =         hello
    final Matcher matcher = ROUTE_PATTERN.matcher(rule);
    while (matcher.find()) {
      // 获取括号一内的匹配结果
      String separator = matcher.group(1);
      // 获取括号二内的匹配结果
      String content = matcher.group(2);
      // 表达式开始
      if (separator == null || separator.length() == 0) {
        pair = new MatchPair();
        condition.put(content, pair);
      }
      // KV开始
      else if ("&".equals(separator)) {
        if (condition.get(content) == null) {
          pair = new MatchPair();
          condition.put(content, pair);
        } else {
          condition.put(content, pair);
        }
      }
      // KV的Value部分开始
      else if ("=".equals(separator)) {
        if (pair == null)
          throw new ParseException("Illegal route rule \""
                  + rule + "\", The error char '" + separator
                  + "' at index " + matcher.start() + " before \""
                  + content + "\".", matcher.start());

        values = pair.matches;
        values.add(content);
      }
      // KV的Value部分开始
      else if ("!=".equals(separator)) {
        if (pair == null)
          throw new ParseException("Illegal route rule \""
                  + rule + "\", The error char '" + separator
                  + "' at index " + matcher.start() + " before \""
                  + content + "\".", matcher.start());

        values = pair.mismatches;
        values.add(content);
      }
      // KV的Value部分的多个条目
      else if (",".equals(separator)) { // 如果为逗号表示
        if (values == null || values.size() == 0)
          throw new ParseException("Illegal route rule \""
                  + rule + "\", The error char '" + separator
                  + "' at index " + matcher.start() + " before \""
                  + content + "\".", matcher.start());
        values.add(content);
      } else {
        throw new ParseException("Illegal route rule \"" + rule
                + "\", The error char '" + separator + "' at index "
                + matcher.start() + " before \"" + content + "\".", matcher.start());
      }
    }
    return condition;
  }

  public Map<String, ServiceProvider> route(Map<String, ServiceProvider> providers, URL url)
          throws RpcException {
    if (providers == null || providers.size() == 0) {
      return providers;
    }
    try {
      if (!matchWhen(url)) {
        return providers;
      }
      Map<String, ServiceProvider> result = new ConcurrentHashMap<String, ServiceProvider>();
      if (thenCondition == null) {
        logger.log(Level.FINE, "The current consumer in the service blacklist. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey());
        return result;
      }
      for (Map.Entry<String, ServiceProvider> entry : providers.entrySet()) {
        if (matchThen(entry.getValue().getUrl(), url)) {
          result.put(entry.getKey(), entry.getValue());
        }
      }
      if (result.size() > 0) {
        return result;
      } else if (force) {
        logger.log(Level.FINE, "The route result is empty and force execute. consumer: " + NetUtils.getLocalHost()
                + ", service: " + url.getServiceKey() + ", router: " + url.getParameterAndDecoded(Constants.RULE_KEY));
        return result;
      }
    } catch (Throwable t) {
      logger.log(Level.SEVERE, "Failed to execute condition router rule: " + getUrl() + ", invokers: " + providers + ", cause: " + t.getMessage(), t);
    }
    return providers;
  }

  public URL getUrl() {
    return url;
  }

  public int compareTo(Router o) {
    if (o == null || o.getClass() != ConditionRouter.class) {
      return 1;
    }
    ConditionRouter c = (ConditionRouter) o;
    return this.priority == c.priority ? url.toFullString().compareTo(c.url.toFullString()) : (this.priority > c.priority ? 1 : -1);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConditionRouter that = (ConditionRouter) o;

    if (priority != that.priority) return false;
    if (force != that.force) return false;
    if (url != null ? !url.equals(that.url) : that.url != null) return false;
    if (whenCondition != null ? !whenCondition.equals(that.whenCondition) : that.whenCondition != null) return false;
    return thenCondition != null ? thenCondition.equals(that.thenCondition) : that.thenCondition == null;
  }

  @Override
  public int hashCode() {
    int result = url != null ? url.hashCode() : 0;
    result = 31 * result + priority;
    result = 31 * result + (force ? 1 : 0);
    result = 31 * result + (whenCondition != null ? whenCondition.hashCode() : 0);
    result = 31 * result + (thenCondition != null ? thenCondition.hashCode() : 0);
    return result;
  }

  public boolean matchWhen(URL url) {
    if (whenCondition == null || whenCondition.size() == 0) {
      return true;// 如果匹配条件为空，表示对所有消费方应用
    }
    return matchCondition(whenCondition, url, null);
  }

  public boolean matchThen(URL url, URL param) {
    return thenCondition != null && matchCondition(thenCondition, url, param);
  }

  private boolean matchCondition(Map<String, MatchPair> condition, URL url, URL param) {
    Map<String, String> sample = url.toMap();
    for (Map.Entry<String, String> entry : sample.entrySet()) {
      String key = entry.getKey();
      MatchPair pair = condition.get(key);
      if (pair != null && !pair.isMatch(entry.getValue(), param)) {
        return false;
      }
    }
    return true;
  }

  private static final class MatchPair {
    final Set<String> matches = new HashSet<String>();
    final Set<String> mismatches = new HashSet<String>();

    public boolean isMatch(String value, URL param) {
      if (!matches.isEmpty() && mismatches.isEmpty()) {
        for (String match : matches) {
          if (UrlUtils.isMatchGlobPattern(match, value, param)) {
            return true;
          }
        }
        return false;
      }

      if (!mismatches.isEmpty() && matches.isEmpty()) {
        for (String mismatch : mismatches) {
          if (UrlUtils.isMatchGlobPattern(mismatch, value, param)) {
            return false;
          }
        }
        return true;
      }

      if (!matches.isEmpty() && !mismatches.isEmpty()) {
        for (String mismatch : mismatches) {
          if (UrlUtils.isMatchGlobPattern(mismatch, value, param)) {
            return false;
          }
        }
        for (String match : matches) {
          if (UrlUtils.isMatchGlobPattern(match, value, param)) {
            return true;
          }
        }
        return false;
      }

      return false;
    }
  }
}
