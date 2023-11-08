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
package com.orientsec.grpc.common.enums;

/**
 * 数据类型
 *
 * @author sxp
 * @since V1.0 2017/3/23
 */
public enum DataType {
  STRING("String"),  // 字符串
  INTEGER("int"),  // 整数
  LONG("Long"),  // 长整数
  DOUBLE("Double"),  // 浮点数
  DATE("Date"),  // 日期
  BOOLEAN("boolean"); //布尔

  private String simpleName;

  DataType(String simpleName) {
    this.simpleName = simpleName;
  }

  /**
   * 获取简单名称
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getSimpleName() {
    return simpleName;
  }

  /**
   * 设置简单名称
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setSimpleName(String simpleName) {
    this.simpleName = simpleName;
  }

}
