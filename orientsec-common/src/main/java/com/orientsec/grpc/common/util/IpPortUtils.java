package com.orientsec.grpc.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IP:port工具类
 *
 * @author sxp
 * @since 2019/12/19
 */
public class IpPortUtils {
  /**
   * IP:port匹配模式
   * <p>
   * 示例：192.168.106.3:50062
   * </p>
   */
  private static final Pattern IP_PORT_PATTERN = Pattern
          .compile("((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?):(\\d+)");


  /**
   * 从字符串中提取IP:port
   *
   * @author sxp
   * @since 2019/12/19
   */
  public static String getAddress(String message) {
    Matcher m = IP_PORT_PATTERN.matcher(message);

    String ipAndPort = null;
    if (m.find()) {
      ipAndPort = m.group();
    }

    return ipAndPort;
  }
}
