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
import com.orientsec.grpc.common.exception.BusinessException;

/**
 * grpc工具类
 *
 * @author sxp
 * @since 2018/3/29
 */
public final class GrpcUtils {
  /**
   * 根据全路径方法名获取接口名
   * <p>
   * grpc中的全路径方法名为：interfaceName/methodName <br>
   * 详见io.grpc.MethodDescriptor#generateFullMethodName(String fullServiceName, String methodName)
   * <p/>
   *
   * @author sxp
   * @since 2018-3-29
   * @since 2018-6-10 用substring代替spilt，提高性能
   */
  public static String getInterfaceNameByFullMethodName(String fullMethodName) {
    Preconditions.checkNotNull(fullMethodName, "fullMethodName");

    int index = fullMethodName.indexOf("/");
    if (index < 0) {
      throw new BusinessException("全路径方法名[" + fullMethodName + "]中不含有[/]字符，请检查！");
    }

    return fullMethodName.substring(0, index);
  }

  /**
   * 根据全路径方法名获取方法名
   * <p>
   * grpc中的全路径方法名为：interfaceName/methodName <br>
   * 详见io.grpc.MethodDescriptor#generateFullMethodName(String fullServiceName, String methodName)
   * <p/>
   *
   * @author sxp
   * @since 2018-3-29
   * @since 2018-6-10 用substring代替spilt，提高性能
   */
  public static String getSimpleMethodName(String fullMethodName) {
    Preconditions.checkNotNull(fullMethodName, "fullMethodName");

    int index = fullMethodName.indexOf("/");
    if (index < 0) {
      throw new BusinessException("全路径方法名[" + fullMethodName + "]中不含有[/]字符，请检查！");
    }

    return fullMethodName.substring(index + 1);
  }


  /**
   * 根据全路径方法名获取接口名(不抛出异常)
   *
   * @author sxp
   * @since 2018-6-5
   * @since 2018-6-10 用substring代替spilt，提高性能
   */
  public static String getInterfaceNameNoneException(String fullMethodName) {
    if (StringUtils.isEmpty(fullMethodName)) {
      return "";
    }

    int index = fullMethodName.indexOf("/");
    if (index < 0) {
      return "";
    }

    return fullMethodName.substring(0, index);
  }

}
