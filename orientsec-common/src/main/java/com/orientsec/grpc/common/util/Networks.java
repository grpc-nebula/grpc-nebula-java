package com.orientsec.grpc.common.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * 网络相关工具类
 *
 * @author sxp
 * @since 2019/11/19
 */
public class Networks {

  /**
   * 根据SocketAddress获取host:port字符串
   *
   * @author sxp
   * @since 2019/11/19
   */
  public static String getHostAndPort(SocketAddress address) {
    if (address instanceof InetSocketAddress) {
      InetSocketAddress inetAddress = (InetSocketAddress) address;

      InetAddress addr = inetAddress.getAddress();
      int port = inetAddress.getPort();

      String ip = addr.getHostAddress();
      if (StringUtils.isEmpty(ip)) {
        return null;
      }

      return ip + ":" + port;
    }

    return null;
  }
}
