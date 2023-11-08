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

import com.orientsec.grpc.common.exception.BusinessException;

import java.math.BigDecimal;

/**
 * 数学工具类
 *
 * @author sxp
 * @since V1.0 Jun 30, 2016
 */
public final class MathUtils {
  /**
   * 判定一个字符串是不是数值
   *
   * @author sxp
   * @since V1.0 Jun 30, 2016
   */
  public static boolean isNumber(String numberString) {
    try {
      Double.parseDouble(numberString);
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  /**
   * 判定一个字符串是不是整数
   *
   * @author sxp
   * @since V1.0 Jun 30, 2016
   */
  public static boolean isInteger(String numberString) {
    try {
      Integer.parseInt(numberString);
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  /**
   * 判定一个字符串是不是长整型
   *
   * @author sxp
   * @since V1.0 2017-4-26
   */
  public static boolean isLong(String numberString) {
    try {
      Long.parseLong(numberString);
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  /**
   * 将字符串转成整数，如果字符串不是数字返回0
   *
   * @author sxp
   * @since 2018-7-19
   */
  public static int parseInt(String numberString) {
    try {
      return Integer.parseInt(numberString);
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  /**
   * 将字符串转成长整数，如果字符串不是数字返回0
   *
   * @author sxp
   * @since 2018-7-19
   */
  public static long parseLong(String numberString) {
    try {
      return Long.parseLong(numberString);
    } catch (NumberFormatException ex) {
      return 0L;
    }
  }

  /**
   * 四舍五入函数
   *
   * @param sourceNum 原来的数字
   * @param scale 需要保留的精度，即小数点后的位数
   * @author sxp
   * @since Jun 30, 2016
   * @since 2018-08-29 删除原来先保留20位小数的的两行代码
   */
  public static double round(double sourceNum, int scale) {
    BigDecimal sourceBD = new BigDecimal(Double.toString(sourceNum));
    BigDecimal one = new BigDecimal("1");

    // ROUND_HALF_UP
    // 向“最接近的”数字舍入，如果与两个相邻数字的距离相等，则舍入远离零的方向（数轴上方向上）。
    // 注意，这是我们大多数人在小学时就学过的舍入模式，即四舍五入。

    return sourceBD.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
  }


  /**
   * 将数字或字符串转换为整数
   *
   * @author sxp
   * @since 2018/5/25
   */
  public static int castToInt(Object value) {
    if (value == null) {
      return 0;
    }

    if (value instanceof Integer) {
      return ((Integer) value).intValue();
    }

    if (value instanceof Number) {
      return ((Number) value).intValue();
    }

    if (value instanceof String) {
      String strVal = (String) value;

      if (strVal.length() == 0 //
              || "null".equals(strVal) //
              || "NULL".equals(strVal)) {
        return 0;
      }

      if (strVal.indexOf(',') != 0) {
        strVal = strVal.replaceAll(",", "");
      }

      return Integer.parseInt(strVal);
    }

    if (value instanceof Boolean) {
      return ((Boolean) value).booleanValue() ? 1 : 0;
    }

    throw new BusinessException("can not cast to int, value : " + value);
  }

  /**
   * 将数字或字符串转换为长整数
   *
   * @author sxp
   * @since 2018/5/25
   */
  public static long castToLong(Object value) {
    if (value == null) {
      return 0;
    }

    if (value instanceof Number) {
      return ((Number) value).longValue();
    }

    if (value instanceof String) {
      String strVal = (String) value;
      if (strVal.length() == 0 //
              || "null".equals(strVal) //
              || "NULL".equals(strVal)) {
        return 0;
      }

      if (strVal.indexOf(',') != 0) {
        strVal = strVal.replaceAll(",", "");
      }

      try {
        return Long.parseLong(strVal);
      } catch (NumberFormatException ex) {
        //
      }
    }

    throw new BusinessException("can not cast to long, value : " + value);
  }

  /**
   * 将数字或字符串转换为Double
   *
   * @author sxp
   * @since 2018/5/25
   */
  public static double castToDouble(Object value) {
    if (value == null) {
      return 0;
    }

    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }

    if (value instanceof String) {
      String strVal = value.toString();
      if (strVal.length() == 0 //
              || "null".equals(strVal) //
              || "NULL".equals(strVal)) {
        return 0;
      }

      if (strVal.indexOf(',') != 0) {
        strVal = strVal.replaceAll(",", "");
      }

      return Double.parseDouble(strVal);
    }

    throw new BusinessException("can not cast to double, value : " + value);
  }

  /**
   * 将数字或字符串转换为整数，转换过程中如果出现异常则返回0
   *
   * @author sxp
   * @since 2018/5/25
   */
  public static int castToIntNoException(Object value) {
    try {
      return castToInt(value);
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * 将数字或字符串转换为长整数，转换过程中如果出现异常则返回0
   *
   * @author sxp
   * @since 2018/5/25
   */
  public static long castToLongNoException(Object value) {
    try {
      return castToLong(value);
    } catch (Exception e) {
      return 0L;
    }
  }

  /**
   * 将数字或字符串转换为Double，转换过程中如果出现异常则返回0
   *
   * @author sxp
   * @since 2018/5/25
   */
  public static double castToDoubleNoException(Object value) {
    try {
      return castToDouble(value);
    } catch (Exception e) {
      return 0D;
    }
  }

}
