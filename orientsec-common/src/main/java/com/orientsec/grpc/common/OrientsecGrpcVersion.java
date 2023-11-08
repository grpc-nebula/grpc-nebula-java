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
package com.orientsec.grpc.common;

/**
 * 东方证券grpc版本号
 *
 * @author sxp
 * @since V1.0 2017/5/23
 */
public class OrientsecGrpcVersion {
  private static final String DEFAULT_VERSION = "1.2.8";// 默认版本号
  /**
   * 版本号
   */
  public static final String VERSION = initVersion();



  /**
   * 初始化版本号
   *
   * @author sxp
   * @since 2018-5-23
   * @since 2018-5-24 先声明logger和DEFAULT_VERSION，然后再声明VERSION
   * @since 2019-11-9 modify by sxp 为了区分不同语言版本的框架，增加语言作为前缀
   * @since 2020-5-20 modify by sxp 如果程序打包方式是”将所有依赖解压为class文件，然后打一个总的jar包"，那么我们原来的解析方式就会出错
   */
  private static String initVersion() {
    String version = "java-" + DEFAULT_VERSION;
    return version;
  }


}
