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



import java.util.*;

/**
 * 集合工具类
 *
 * @author heiden
 * @since 2018/3/15
 */
public class CollectionUtils {

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <T> List<T> sort(List<T> list) {
    if (list != null && list.size() > 0) {
      Collections.sort((List) list);
    }
    return list;
  }

  /**
   * 简单名称比较器
   * <p>
   * 使用场景描述：com.xxxxxxx.Apple 与 com.aaaaaaa.IBM 比较，<br>
   * 要根据去掉package后的类名Apple,IBM进行比较
   * </p>
   *
   * @author sxp
   * @since 2018/12/1
   */
  private static final Comparator<String> SIMPLE_NAME_COMPARATOR = new Comparator<String>() {
    public int compare(String s1, String s2) {
      if (s1 == null && s2 == null) {
        return 0;
      }
      if (s1 == null) {
        return -1;
      }
      if (s2 == null) {
        return 1;
      }
      int i1 = s1.lastIndexOf('.');
      if (i1 >= 0) {
        s1 = s1.substring(i1 + 1);
      }
      int i2 = s2.lastIndexOf('.');
      if (i2 >= 0) {
        s2 = s2.substring(i2 + 1);
      }
      return s1.compareToIgnoreCase(s2);
    }
  };

  /**
   * 根据简单名称进行排序
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static List<String> sortSimpleName(List<String> list) {
    if (list != null && list.size() > 0) {
      Collections.sort(list, SIMPLE_NAME_COMPARATOR);
    }
    return list;
  }

  /**
   * 将Map集合中List类型的value值拆分为Map
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static Map<String, Map<String, String>> splitAll(Map<String, List<String>> list, String separator) {
    if (list == null) {
      return null;
    }
    Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
    for (Map.Entry<String, List<String>> entry : list.entrySet()) {
      result.put(entry.getKey(), split(entry.getValue(), separator));
    }
    return result;
  }

  /**
   * 将Map集合中Map类型的value值组合为List
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static Map<String, List<String>> joinAll(Map<String, Map<String, String>> map, String separator) {
    if (map == null) {
      return null;
    }
    Map<String, List<String>> result = new HashMap<String, List<String>>();
    for (Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
      result.put(entry.getKey(), join(entry.getValue(), separator));
    }
    return result;
  }

  /**
   * 将List拆分为Map
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static Map<String, String> split(List<String> list, String separator) {
    if (list == null) {
      return null;
    }
    Map<String, String> map = new HashMap<String, String>();
    if (list == null || list.size() == 0) {
      return map;
    }
    for (String item : list) {
      int index = item.indexOf(separator);
      if (index == -1) {
        map.put(item, "");
      } else {
        map.put(item.substring(0, index), item.substring(index + 1));
      }
    }
    return map;
  }

  /**
   * 将Map合并为List
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static List<String> join(Map<String, String> map, String separator) {
    if (map == null) {
      return null;
    }
    List<String> list = new ArrayList<String>();
    if (map == null || map.size() == 0) {
      return list;
    }
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (value == null || value.length() == 0) {
        list.add(key);
      } else {
        list.add(key + separator + value);
      }
    }
    return list;
  }

  /**
   * 将List中的字符串拼接起来返回一个字符串
   * <p>
   *  可以指定各元素之间的分隔符
   * </p>
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static String join(List<String> list, String separator) {
    StringBuilder sb = new StringBuilder();
    for (String ele : list) {
      if (sb.length() > 0) {
        sb.append(separator);
      }
      sb.append(ele);
    }
    return sb.toString();
  }

  /**
   * 比较两个Map集合的键值对是否相同
   * <p>
   * 而不管Map的类型，存储方式，是否是线程同步的等等。<br>
   * 只关注其中的数据
   * </p>
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean mapEquals(Map<?, ?> map1, Map<?, ?> map2) {
    if (map1 == null && map2 == null) {
      return true;
    }
    if (map1 == null || map2 == null) {
      return false;
    }
    if (map1.size() != map2.size()) {
      return false;
    }
    for (Map.Entry<?, ?> entry : map1.entrySet()) {
      Object key = entry.getKey();
      Object value1 = entry.getValue();
      Object value2 = map2.get(key);
      if (!objectEquals(value1, value2)) {
        return false;
      }
    }
    return true;
  }

  /**
   * 加强型的Object类型对象比较
   * <p>
   *  增加了对null的判断
   * </p>
   *
   * @author sxp
   * @since 2018/12/1
   */
  private static boolean objectEquals(Object obj1, Object obj2) {
    if (obj1 == null && obj2 == null) {
      return true;
    }
    if (obj1 == null || obj2 == null) {
      return false;
    }
    return obj1.equals(obj2);
  }

  /**
   * 将字符串数组转化为Map
   * <p>
   * 依次为key1,value1,key2,value2
   * </p>
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static Map<String, String> toStringMap(String... pairs) {
    Map<String, String> parameters = new HashMap<String, String>();
    if (pairs.length > 0) {
      if (pairs.length % 2 != 0) {
        throw new IllegalArgumentException("pairs must be even.");
      }
      for (int i = 0; i < pairs.length; i = i + 2) {
        parameters.put(pairs[i], pairs[i + 1]);
      }
    }
    return parameters;
  }

  /**
   * 将Object数组转化为Map
   * <p>
   * 依次为key1,value1,key2,value2
   * </p>
   *
   * @author sxp
   * @since 2018/12/1
   */
  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> toMap(Object... pairs) {
    Map<K, V> ret = new HashMap<K, V>();
    if (pairs == null || pairs.length == 0) return ret;

    if (pairs.length % 2 != 0) {
      throw new IllegalArgumentException("Map pairs can not be odd number.");
    }
    int len = pairs.length / 2;
    for (int i = 0; i < len; i++) {
      ret.put((K) pairs[2 * i], (V) pairs[2 * i + 1]);
    }
    return ret;
  }

  /**
   * 集合是否为空
   * <p>
   * null视为空
   * </p>
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean isEmpty(Collection<?> collection) {
    return collection == null || collection.size() == 0;
  }

  /**
   * 集合是否非空
   * <p>
   * null视为空
   * </p>
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean isNotEmpty(Collection<?> collection) {
    return collection != null && collection.size() > 0;
  }

  /**
   * 禁止创建该类的实例
   *
   * @author sxp
   * @since 2018/12/1
   */
  private CollectionUtils() {
  }

}
