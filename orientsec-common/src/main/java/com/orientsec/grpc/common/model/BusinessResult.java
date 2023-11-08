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
package com.orientsec.grpc.common.model;

/**
 * 业务操作结果
 * <p>
 *  封装了操作是否成功的布尔值，以及操作过程中产生的字符串类型的信息。
 * </p>
 *
 * @author sxp
 * @since V1.0 2017/3/23
 */
public class BusinessResult {
  private boolean success;// 操作是否成功true|false
  private String message;// 操作返回的消息

  public BusinessResult() {
    super();
  }

  public BusinessResult(boolean success, String message) {
    super();
    this.success = success;
    this.message = message;
  }

  /**
   * 获取成功标志
   *
   * @author sxp
   * @since 2018/12/1
   */
  public boolean isSuccess() {
    return success;
  }

  /**
   * 设置成功标志
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setSuccess(boolean success) {
    this.success = success;
  }

  /**
   * 获取结果信息
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getMessage() {
    return message;
  }

  /**
   * 设置结果信息
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setMessage(String message) {
    this.message = message;
  }


  @Override
  public String toString() {
    return "BusinessResult [success=" + success + ", message=" + message + "]";
  }

}
