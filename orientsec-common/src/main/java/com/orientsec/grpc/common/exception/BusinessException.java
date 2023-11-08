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
package com.orientsec.grpc.common.exception;

/**
 * 业务异常
 * <p>
 * 继承自RuntimeException，不需要捕捉。
 * </p>
 *
 * @author sxp
 * @since V1.0 2017-3-27
 */
public class BusinessException extends RuntimeException {
  private static final long serialVersionUID = -6253253200433098358L;
  private int errorCode;

  public BusinessException() {
    super();
  }

  public BusinessException(String message) {
    super(message);
  }

  public BusinessException(String message, Throwable cause) {
    super(message, cause);
  }

  public BusinessException(Throwable cause) {
    super(cause);
  }

  /**
   * 自定义错误代码的异常构造函数
   *
   * @author sxp
   * @since 2018/12/1
   */
  public BusinessException(int errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public BusinessException(int errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  /**
   * 获取错误代码
   *
   * @author sxp
   * @since 2018/12/1
   */
  public int getErrorCode() {
    return errorCode;
  }

  /**
   * 设置错误代码
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }

}
