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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 集群地址工具类
 * <p>
 * 集群地址指的是这种格式的字符串： 168.61.2.23:2181,168.61.2.24:2181,168.61.2.25:2181
 * </p>
 *
 * @author sxp
 * @since 2018/7/10
 */
public class ClusterAddressUtils {

  /**
   * 匹配这种格式的IP地址和端口组成的字符串: host:port
   * <p>IP只支持IP4</p>
   */
  private static final Pattern HOST_PORT_PATTERN = Pattern.compile("([0-9.]+):([0-9]+)");// 修改请谨慎
  //private static final Pattern HOST_PORT_PATTERN = Pattern.compile("((((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?))):([0-9]+)");

  /**
   * 将集群地址转换为InetSocketAddress的列表
   *
   * @param addressesString 这种格式的字符串 168.61.2.23:2181,168.61.2.24:2181,168.61.2.25:2181
   * @author sxp
   * @since 2018/7/10
   */
  public static List<InetSocketAddress> getAddresses(String addressesString) {
    if (StringUtils.isEmpty(addressesString)) {
      return null;
    }

    addressesString = addressesString.trim();
    List<String> addressList = Arrays.asList(addressesString.split("\\s*,\\s*"));

    return getAddressesByList(addressList);
  }

  /**
   * 将集群地址转换为InetSocketAddress的列表
   *
   * @author sxp
   * @since 2018/7/10
   */
  private static List<InetSocketAddress> getAddressesByList(List<String> addressList) {
    List<InetSocketAddress> result = new ArrayList<InetSocketAddress>();

    String host;
    int port;

    for (String address : addressList) {
      if (StringUtils.isEmpty(address)) {
        continue;
      }
      try {
        host = getHost(address);
        port = getPort(address);
        if (StringUtils.isEmpty(host)) {
          continue;
        }

        InetSocketAddress inetAddress = new InetSocketAddress(host, port);

        if (inetAddress.isUnresolved()) {
          continue;
        } else {
          result.add(inetAddress);
        }
      } catch (Throwable t) {
        continue;
      }
    }

    return result;
  }


  /**
   * 提取IP地址
   */
  private static String getHost(String address) {
    Matcher matcher = HOST_PORT_PATTERN.matcher(address);
    return matcher.matches() ? matcher.group(1) : null;
  }

  /**
   * 提取端口
   */
  private static int getPort(String address) {
    Matcher matcher = HOST_PORT_PATTERN.matcher(address);
    return matcher.matches() ? Integer.parseInt(matcher.group(2)) : 0;
  }


}
