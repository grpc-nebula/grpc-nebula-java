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

import com.orientsec.grpc.common.constant.GlobalConstants;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * properties属性文件工具类
 *
 * @author sxp
 * @since V1.0 2017/3/21
 */
public final class PropertiesUtils {
  /**
   * 获取属性文件的所有属性的对象
   * <p>
   * 优先读取jar外的配置文件；如果jar外没有配置文件，再从jar包内部进行读取。
   * <p/>
   * <p>
   * 应用启动时，按以下顺序依次找配置文件；如果没找到，则顺延到下一条：
   * 1.用户可以通过启动参数-Ddfzq.grpc.config=/xxx/xxx 配置grpc配置文件所在的目录的绝对路径
   * 2.用户从启动目录下的config中查找grpc配置文件
   * 3.用户从启动目录下查找grpc配置文件
   * </p>
   *
   * @param filePath 属性文件的路径（含文件名）
   * @author sxp
   * @since 2018/3/22
   * @since 2018-8-2 modify by sxp 支持配置文件放在自定义目录，或config目录，或当前目录
   */
  public static Properties getProperties(String filePath) throws IOException {
    Properties p;

    String directoryOfConfigFile = System.getProperty(GlobalConstants.SYSTEM_PATH_NAME);

    if (!StringUtils.isEmpty(directoryOfConfigFile)) {// 优先从自定义目录中读取配置文件
      int len = directoryOfConfigFile.length();
      if (len >= 2 && directoryOfConfigFile.endsWith("/")) {
        directoryOfConfigFile = directoryOfConfigFile.substring(0, len - 1);// 删除最后一个字符/
      } else if (len >= 2 && directoryOfConfigFile.endsWith("\\")) {
        directoryOfConfigFile = directoryOfConfigFile.substring(0, len - 1);// 删除最后一个字符\
      }
    } else {
      directoryOfConfigFile = "config";// 没有自定义目录，从config目录中读取配置文件
    }

    // filePath只包含文件名，不含有字符/(也不含有字符\)
    String newFilePath = directoryOfConfigFile + "/" + filePath;

    try {
      try {
        p = getProperties(newFilePath, false);// jar包外
      } catch (IOException e) {
        p = getProperties(newFilePath, true);// jar包内
      }
    } catch (IOException e) {
      // 最后从当前目录下读取配置文件
      try {
        p = getProperties(filePath, false);// jar包外
      } catch (IOException e2) {
        p = getProperties(filePath, true);// jar包内
      }
    }


    return p;
  }

  /**
   * 获取属性文件的所有属性的对象
   * <p>
   * 支持配置文件在jar包内和jar包外
   * </p>
   *
   * @param filePath    属性文件的路径（含文件名）
   * @param isFileInJar true表示配置文件在jar包内，false表示在jar包外
   * @author sxp
   * @since V1.0 2017/3/22
   * @since 2018-8-8 modify by sxp 区分绝对路径和相对路径
   */
  private static Properties getProperties(String filePath, boolean isFileInJar) throws IOException {
    Properties p = new Properties();
    InputStream is;

    if (isFileInJar) {
      is = PropertiesUtils.class.getClassLoader().getResourceAsStream(filePath);
    } else {

      String absolutePath;
      if (filePath.startsWith("/") || filePath.contains(":")) {// linux与windows的绝对路径特征
        absolutePath = filePath;
      } else {
        absolutePath = System.getProperty("user.dir") + "/" + filePath;
      }

      is = new FileInputStream(absolutePath);
    }

    if (is == null) {
      throw new IOException("PropertiesUtils.getProperties:无法读取属性文件，请确认属性文件的路径["
              + filePath + "]是否正确！");
    }

    // 解决参数值中文乱码问题
    // p.load(is);
    Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
    p.load(reader);

    reader.close();
    is.close();

    return p;
  }

  /**
   * 从属性对象中获取有效的整数类型数值
   * <p>
   * 如果属性对象中不含有对应key值，或者属性值不是整数，返回默认值
   * </p>
   *
   * @author sxp
   * @since 2018/6/7
   */
  public static int getValidIntegerValue(Properties properties, String key, int defaultValue) {
    if (properties == null) {
      return defaultValue;
    }

    int result = defaultValue;

    if (properties.containsKey(key)) {
      String value = properties.getProperty(key);
      if (value != null) {
        value = value.trim();
      }
      if (MathUtils.isInteger(value)) {
        result = Integer.parseInt(value);
      }
    }

    return result;
  }

  /**
   * 从属性对象中获取有效的长整数类型数值
   * <p>
   * 如果属性对象中不含有对应key值，或者属性值不是长整数，返回默认值
   * </p>
   *
   * @author sxp
   * @since 2018-7-7
   */
  public static long getValidLongValue(Properties properties, String key, long defaultValue) {
    if (properties == null) {
      return defaultValue;
    }

    long result = defaultValue;

    if (properties.containsKey(key)) {
      String value = properties.getProperty(key);
      if (value != null) {
        value = value.trim();
      }
      if (MathUtils.isLong(value)) {
        result = Long.parseLong(value);
      }
    }

    return result;
  }


  /**
   * 从属性对象中获取有效的double类型数值
   * <p>
   * 如果属性对象中不含有对应key值，或者属性值不是double，返回默认值
   * </p>
   *
   * @author sxp
   * @since 2019/2/11
   */
  public static double getValidDoubleValue(Properties properties, String key, double defaultValue) {
    if (properties == null) {
      return defaultValue;
    }

    double result = defaultValue;

    if (properties.containsKey(key)) {
      String value = properties.getProperty(key);
      if (value != null) {
        value = value.trim();
      }
      if (MathUtils.isNumber(value)) {
        result = Double.parseDouble(value);
      }
    }

    return result;
  }

  /**
   * 从属性对象中获取Stringe类型参数值
   *
   * @author sxp
   * @since 2019/2/12
   */
  public static String getStringValue(Properties properties, String key, String defaultValue) {
    if (properties == null) {
      return defaultValue;
    }

    String result = defaultValue;

    if (properties.containsKey(key)) {
      String value = properties.getProperty(key);
      if (value != null) {
        value = value.trim();
      }
      result = value;
    }

    return result;
  }

  /**
   * 从配置文件中读取key的value值为boolean类型的值，如果没有返回默认值
   *
   * @atuher yulei
   * @since 2019/8/21
   */
  public static boolean getValidBooleanValue(Properties properties, String key, boolean defaultValue) {
    if (properties == null) {
      return defaultValue;
    }

    boolean result = defaultValue;

    if (properties.containsKey(key)) {
      String value = properties.getProperty(key);
      if (value != null) {
        value = value.trim();
      }
      result = Boolean.parseBoolean(value);
    }

    return result;
  }

}
