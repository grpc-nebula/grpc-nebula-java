/*
 * Copyright 2018-2019 The Apache Software Foundation
 * Modifications 2019 Orient Securities Co., Ltd.
 * Modifications 2019 BoCloud Inc.
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
package com.orientsec.grpc.registry.common;

import java.util.Comparator;

/**
 * 根据URL的IP进行比较器
 *
 * @author heiden
 * @since 2018/3/15
 */
public class UrlIpComparator implements Comparator<URL> {
  @Override
  public int compare(URL first, URL second) {
    //对象为空校验
    if (null == first && null == second) {
      return 0;
    } else if (null == first) {
      return -1;
    } else if (null == second) {
      return 1;
    }

    String firstIp = first.getIp();
    String secendIp = second.getIp();

    if (firstIp == secendIp) {
      return 0;
    }

    // return a negative integer, zero, or a positive integer as the first
    // argument is less than, equal to, or greater than the second.
    if (firstIp != null && secendIp != null) {
      return firstIp.compareTo(secendIp);
    } else if (firstIp != null && secendIp == null) {
      return 1;
    } else if (firstIp == null && secendIp != null) {
      return -1;
    }

    return 0;
  }

}
