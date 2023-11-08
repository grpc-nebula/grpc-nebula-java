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
package com.orientsec.grpc.common.util;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * 散列表工具类
 *
 * @author sxp
 * @since V1.0 2017/3/28
 */
public final class MapUtils {
  /**
   * The largest power of two that can be represented as an {@code int}.
   */
  public static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);

  /**
   * 根据UnmodifiableMap对象生成一个普通的Map对象
   * <p>
   * 对UnmodifiableMap进行遍历，复制里面的对象至新的Map对象
   * </p>
   *
   * @author sxp
   * @since V1.0 2017-3-28
   */
  public static <K, V> Map<K, V> getMapFromUnmodifiableMap(Map<K, V> source) {
    Preconditions.checkNotNull(source, "source");

    Map<K, V> dest = new HashMap<K, V>(source.size());

    for (Map.Entry<K, V> entry : source.entrySet()) {
      dest.put(entry.getKey(), entry.getValue());
    }

    return dest;
  }

  /**
   * 将一个Map中的数据拷贝至另一个对象
   * <p>
   * 浅克隆
   * </p>
   *
   * @author sxp
   * @since V1.0 2017-04-24
   */
  public static <K, V> Map<K, V> mapCopy(Map<K, V> source, Map<K, V> dest) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(dest, "dest");

    for (Map.Entry<K, V> entry : source.entrySet()) {
      dest.put(entry.getKey(), entry.getValue());
    }

    return dest;
  }

  /**
   * 从散列表中取对应key值对应的value
   *
   * @return 如果从map中取出的值为null返回默认值defaultValue，如果map为null返回默认值defaultValue
   * @author sxp
   * @since 2018-6-30
   */
  public static <K, V> V getValue(Map<K, V> map, K key, V defaultValue) {
    if (map == null) {
      return defaultValue;
    }

    V value = map.get(key);

    if (value == null) {
      value = defaultValue;
    }

    return value;
  }

  /**
   * 从散列表中取对应key值对应的value然后转换成int值
   *
   * @return 如果从map中取出的值为null或非整型返回默认值defaultValue，如果map为null返回默认值defaultValue
   * @author sxp
   * @since 2018-6-30
   */
  public static int getValue2Int(Map<String, String> map, String key, int defaultValue) {
    if (map == null) {
      return defaultValue;
    }

    String value = map.get(key);

    if (StringUtils.isEmpty(value)) {
      return defaultValue;
    } else {
      try {
        return Integer.parseInt(value);
      } catch (Exception e) {
        return defaultValue;
      }
    }
  }

  /**
   * 从散列表中取对应key值对应的value然后转换成long值
   *
   * @return 如果从map中取出的值为null或非长整型返回默认值defaultValue，如果map为null返回默认值defaultValue
   * @author sxp
   * @since 2018-6-30
   */
  public static long getValue2Long(Map<String, String> map, String key, long defaultValue) {
    if (map == null) {
      return defaultValue;
    }

    String value = map.get(key);

    if (StringUtils.isEmpty(value)) {
      return defaultValue;
    } else {
      try {
        return Long.parseLong(value);
      } catch (Exception e) {
        return defaultValue;
      }
    }
  }

  /**
   * 根据期望大小获取Map的容量大小
   * <p>
   * Returns a capacity that is sufficient to keep the map from being resized as
   * long as it grows no larger than expectedSize and the load factor is >= its
   * default (0.75).
   * </p>
   *
   * @author sxp
   * @since 2019-01-30
   */
  public static int capacity(int expectedSize) {
    if (expectedSize < 3) {
      if (expectedSize < 0) {
        throw new IllegalArgumentException("expectedSize cannot be negative but was: " + expectedSize);
      }
      return expectedSize + 1;
    }

    if (expectedSize < MAX_POWER_OF_TWO) {
      // This is the calculation used in JDK8 to resize when a putAll
      // happens; it seems to be the most conservative calculation we
      // can make.  0.75 is the default load factor.
      return (int) ((float) expectedSize / 0.75F + 1.0F);
    }

    return Integer.MAX_VALUE;
  }
}
