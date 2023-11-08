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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 读取文本文件
 *
 * @author sxp
 * @since 2018-5-17
 */
public final class ReadTextFileUtil {
  /**
   * 行分隔符
   * <p>
   * Linux下为\n <br>
   * Windows下为\r\n <br>
   * </p>
   */
  public static final String LINE_SEPARATOR = System.getProperties().getProperty("line.separator");

  /**
   * 读取文本文件的内容至一个字符串
   *
   * @author sxp
   * @since 2018-5-17
   */
  public static String readContent(String filePath) throws Exception {
    File file = new File(filePath);
    if (file == null || !file.exists()) {
      throw new Exception("读取文本文件时，指定的文件路径[" + filePath + "]不正确！");
    }

    FileReader reader = new FileReader(filePath);
    BufferedReader bf = new BufferedReader(reader);
    StringBuilder sb = new StringBuilder();
    String line;

    while ((line = bf.readLine()) != null) {
      sb.append(line);
      sb.append(LINE_SEPARATOR);
    }

    bf.close();
    reader.close();

    return sb.toString();
  }

  /**
   * 读取文本文件的内容至一个字符串(入参为File)
   *
   * @author sxp
   * @since 2018-5-17
   */
  public static String readContent(File file) throws Exception {
    return readContent(file.getAbsolutePath());
  }

  /**
   * 读取文本文件的内容至一个列表
   *
   * @author sxp
   * @since 2018-5-17
   */
  public static List<String> readContent2List(String filePath) throws Exception {
    List<String> result = new ArrayList<String>();

    File file = new File(filePath);
    if (file == null || !file.exists()) {
      throw new Exception("读取文本文件时，指定的文件路径[" + filePath + "]不正确！");
    }

    FileReader reader = new FileReader(filePath);
    BufferedReader bf = new BufferedReader(reader);
    String line;

    while ((line = bf.readLine()) != null) {
      result.add(line);
    }

    bf.close();
    reader.close();

    return result;
  }

}
