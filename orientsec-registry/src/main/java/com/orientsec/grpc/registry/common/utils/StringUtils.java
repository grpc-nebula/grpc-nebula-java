/*
 * Copyright 2018-2019 The Apache Software Foundation
 * Modifications 2019 Orient Securities Co., Ltd.
 * Modifications 2019 BoCloud Inc.
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
package com.orientsec.grpc.registry.common.utils;



import com.orientsec.grpc.registry.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 *
 * @author heiden
 * @since 2018/3/15.
 *@since  modify by wjw 2017/4/14 change Logger from  org.slf4j.Logger to java.util.logging.Logger
 */
public class StringUtils {
  public static final String[] EMPTY_STRING_ARRAY = new String[0];
  private static final Logger logger = LoggerFactory.getLogger(StringUtils.class);
  private static final Pattern KVP_PATTERN = Pattern.compile("([_.a-zA-Z0-9][-_.a-zA-Z0-9]*)[=](.*)"); //key value pair pattern.

  private static final Pattern INT_PATTERN = Pattern.compile("^\\d+$");

  /**
   * 是否为空字符串
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean isBlank(String str) {
    if (str == null || str.length() == 0)
      return true;
    return false;
  }

  /**
   * is empty string.
   *
   * @param str source string.
   * @return is empty.
   */
  public static boolean isEmpty(String str) {
    if (str == null || str.length() == 0)
      return true;
    return false;
  }

  /**
   * is not empty string.
   *
   * @param str source string.
   * @return is not empty.
   */
  public static boolean isNotEmpty(String str) {
    return str != null && str.length() > 0;
  }


  /**
   * is integer string.
   *
   * @param str
   * @return is integer
   */
  public static boolean isInteger(String str) {
    if (str == null || str.length() == 0)
      return false;
    return INT_PATTERN.matcher(str).matches();
  }

  public static int parseInteger(String str) {
    if (!isInteger(str))
      return 0;
    return Integer.parseInt(str);
  }

  /**
   * parse query string to Parameters.
   *
   * @param qs query string.
   * @return Parameters instance.
   */
  public static Map<String, String> parseQueryString(String qs) {
    if (qs == null || qs.length() == 0)
      return new HashMap<String, String>();
    return parseKeyValuePair(qs, "\\&");
  }

  /**
   * 根据Map获取服务接口的唯一标识
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static String getServiceKey(Map<String, String> ps) {
    StringBuilder buf = new StringBuilder();
    String group = ps.get(Constants.GROUP_KEY);
    if (group != null && group.length() > 0) {
      buf.append(group).append("/");
    }
    buf.append(ps.get(Constants.INTERFACE_KEY));
    String version = ps.get(Constants.VERSION_KEY);
    if (version != null && version.length() > 0) {
      buf.append(":").append(version);
    }
    return buf.toString();
  }

  /**
   * parse key-value pair.
   *
   * @param str           string.
   * @param itemSeparator item separator.
   * @return key-value map;
   */
  private static Map<String, String> parseKeyValuePair(String str, String itemSeparator) {
    String[] tmp = str.split(itemSeparator);
    Map<String, String> map = new HashMap<String, String>(tmp.length);
    for (int i = 0; i < tmp.length; i++) {
      Matcher matcher = KVP_PATTERN.matcher(tmp[i]);
      if (matcher.matches() == false)
        continue;
      map.put(matcher.group(1), matcher.group(2));
    }
    return map;
  }

  /**
   * 通过Map获取URL中的查询条件字符串
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static String toQueryString(Map<String, String> ps) {
    StringBuilder buf = new StringBuilder();
    if (ps != null && ps.size() > 0) {
      for (Map.Entry<String, String> entry : new TreeMap<String, String>(ps).entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        if (key != null && key.length() > 0
                && value != null && value.length() > 0) {
          if (buf.length() > 0) {
            buf.append("&");
          }
          buf.append(key);
          buf.append("=");
          buf.append(value);
        }
      }
    }
    return buf.toString();
  }

 /**
  * 比较两个字符串是否相等
  *
  * @author sxp
  * @since 2018/12/1
  */
  public static boolean isEquals(String s1, String s2) {
    if (s1 == null && s2 == null)
      return true;
    if (s1 == null || s2 == null)
      return false;
    return s1.equals(s2);
  }

  /**
   * 判断第一个字符串是否包含第二个字符串
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean isContains(String values, String value) {
    if (values == null || values.length() == 0) {
      return false;
    }
    return isContains(Constants.COMMA_SPLIT_PATTERN.split(values), value);
  }

  /**
   * 判断第一个字符串数组中是否包含第二个字符串
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean isContains(String[] values, String value) {
    if (value != null && value.length() > 0 && values != null && values.length > 0) {
      for (String v : values) {
        if (value.equals(v)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 检查字符串的每一个字符是否都为数字
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean isNumeric(String str) {
    if (str == null) {
      return false;
    }
    int sz = str.length();
    for (int i = 0; i < sz; i++) {
      if (Character.isDigit(str.charAt(i)) == false) {
        return false;
      }
    }
    return true;
  }
}
