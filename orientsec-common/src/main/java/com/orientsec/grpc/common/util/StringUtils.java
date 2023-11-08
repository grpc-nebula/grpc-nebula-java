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


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 *
 * @author sxp
 * @since 2018-3-23
 */
public final class StringUtils {

  /**
   * 判断字符串是否为空
   *
   * @author sxp
   * @since 2018-3-23
   */
  public static boolean isEmpty(String str) {
    if (str == null || str.length() == 0) {
      return true;
    }
    return false;
  }

  /**
   * 判断字符串是否非空
   *
   * @author sxp
   * @since 2018/12/4
   */
  public static boolean isNotEmpty(String str) {
    return !isEmpty(str);
  }

  /**
   * 检查字符串是否含有真实的文本
   *
   * <p>
   * 代码来源：org.springframework.util.StringUtils  <br>
   * 只有当字符串不是null，长度大于0，并且至少含有一个非空白字符
   * </p>
   */
  public static boolean hasText(String str) {
    return hasText((CharSequence) str);
  }

  /**
   * Check whether the given {@code CharSequence} contains actual <em>text</em>.
   *
   * <p>
   * 代码来源：org.springframework.util.StringUtils
   * </p>
   * <p>
   * <p>More specifically, this method returns {@code true} if the
   * {@code CharSequence} is not {@code null}, its length is greater than
   * 0, and it contains at least one non-whitespace character.
   * <p>
   * <pre class="code">
   * StringUtils.hasText(null) = false
   * StringUtils.hasText("") = false
   * StringUtils.hasText(" ") = false
   * StringUtils.hasText("12345") = true
   * StringUtils.hasText(" 12345 ") = true
   * </pre>
   *
   * @param str the {@code CharSequence} to check (may be {@code null})
   * @return {@code true} if the {@code CharSequence} is not {@code null},
   * its length is greater than 0, and it does not contain whitespace only
   * @see Character#isWhitespace
   */
  public static boolean hasText(CharSequence str) {
    if (!hasLength(str)) {
      return false;
    }
    int strLen = str.length();
    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(str.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasLength(CharSequence str) {
    return (str != null && str.length() > 0);
  }


  /**
   * 字符串中是否有中文
   *
   * @author sxp
   * @since V1.0 2017-3-23
   */
  public static boolean hasChineseCharacter(String str) {
    // 匹配中文字符的正则表达式： [\u4e00-\u9fa5]
    String reg = "[\\u4E00-\\u9FA5]";
    Pattern p = Pattern.compile(reg);
    Matcher m;

    m = p.matcher(str);

    return m.find();
  }

  /**
   * 去除字符串的左右两侧的空格
   *
   * @author sxp
   * @since 2018-6-7
   */
  public static String trim(String source) {
    if (isEmpty(source)) {
      return source;
    }

    return source.trim();
  }


}
