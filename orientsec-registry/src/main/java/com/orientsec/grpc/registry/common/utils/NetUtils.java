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
package com.orientsec.grpc.registry.common.utils;



import com.orientsec.grpc.registry.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * IP and Port Helper for RPC
 *
 * @author heiden
 * @since 2018/3/15
 * @since  2017/4/14  modify by wjw：change Logger from  org.slf4j.Logger to java.util.logging.Logger
 */
public class NetUtils {
  private static final Logger logger = LoggerFactory.getLogger(NetUtils.class);

  /**
   * 本机地址（回环地址）
   */
  public static final String LOCALHOST = "127.0.0.1";

  /**
   * 匹配任意的IP地址
   */
  public static final String ANYHOST = "0.0.0.0";

  /**
   * 随机端口的最小端口号
   */
  private static final int RND_PORT_START = 30000;

  /**
   * 随机端口的随机数范围的最大值（不包含）
   */
  private static final int RND_PORT_RANGE = 10000;

  /**
   * 随机数对象
   */
  private static final Random RANDOM = new Random(System.currentTimeMillis());

  /**
   * 获取指定范围内的随机端口
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static int getRandomPort() {
    return RND_PORT_START + RANDOM.nextInt(RND_PORT_RANGE);
  }

  /**
   * 获取未被使用额随机端口
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static int getAvailablePort() {
    ServerSocket ss = null;
    try {
      ss = new ServerSocket();
      ss.bind(null);
      return ss.getLocalPort();
    } catch (IOException e) {
      return getRandomPort();
    } finally {
      if (ss != null) {
        try {
          ss.close();
        } catch (IOException e) {
        }
      }
    }
  }

  /**
   * 指定端口号最小值，获取有效的可用端口号
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static int getAvailablePort(int port) {
    if (port <= 0) {
      return getAvailablePort();
    }
    for (int i = port; i < MAX_PORT; i++) {
      ServerSocket ss = null;
      try {
        ss = new ServerSocket(i);
        return i;
      } catch (IOException e) {
        // continue
      } finally {
        if (ss != null) {
          try {
            ss.close();
          } catch (IOException e) {
          }
        }
      }
    }
    return port;
  }

  /**
   * 最小端口号
   */
  private static final int MIN_PORT = 0;

  /**
   * 最大端口号
   */
  private static final int MAX_PORT = 65535;


//  public static boolean isInvalidPort(int port) {
//    return port > MIN_PORT || port <= MAX_PORT;
//  }

  /**
   * 匹配[IP地址:端口]的匹配模式对象
   */
  private static final Pattern ADDRESS_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}\\:\\d{1,5}$");

  /**
   * 是否为有效的[IP地址:端口]
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean isValidAddress(String address) {
    return ADDRESS_PATTERN.matcher(address).matches();
  }

  /**
   * 本机IP地址的匹配模式对象
   *
   * @author sxp
   * @since 2018/12/1
   */
  private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");

  /**
   * 检测主机名（或IP）是否为当前主机名（或IP）
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean isLocalHost(String host) {
    return host != null
            && (LOCAL_IP_PATTERN.matcher(host).matches()
            || host.equalsIgnoreCase("localhost"));
  }

  /**
   * 检测主机名是否为任意主机的通配符
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean isAnyHost(String host) {
    return "0.0.0.0".equals(host);
  }

  /**
   * 检查是否为有效的本机主机名（或IP地址）
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean isInvalidLocalHost(String host) {
    return host == null
            || host.length() == 0
            || host.equalsIgnoreCase("localhost")
            || host.equals("0.0.0.0")
            || (LOCAL_IP_PATTERN.matcher(host).matches());
  }

  /**
   * 检查是否为无效的本机主机名（或IP地址）
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static boolean isValidLocalHost(String host) {
    return !isInvalidLocalHost(host);
  }

  /**
   * 获取制定主机名（或IP）、端口的InetSocketAddress对象
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static InetSocketAddress getLocalSocketAddress(String host, int port) {
    return isInvalidLocalHost(host) ?
            new InetSocketAddress(port) : new InetSocketAddress(host, port);
  }

  /**
   * 地址匹配模式
   */
  private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

  /**
   * 通过IP检测InetAddress是否有效
   *
   * @author sxp
   * @since 2018/12/1
   */
  private static boolean isValidAddress(InetAddress address) {
    if (address == null || address.isLoopbackAddress())
      return false;
    String name = address.getHostAddress();
    return (name != null
            && !ANYHOST.equals(name)
            && !LOCALHOST.equals(name)
            && IP_PATTERN.matcher(name).matches());
  }

  /**
   * 获取当前主机的IP地址
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static String getLocalHost() {
    InetAddress address = getLocalAddress();
    return address == null ? LOCALHOST : address.getHostAddress();
  }

  /**
   * 方法描述
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static String filterLocalHost(String host) {
    if (host == null || host.length() == 0) {
      return host;
    }
    if (host.contains("://")) {
      URL u = URL.valueOf(host);
      if (NetUtils.isInvalidLocalHost(u.getHost())) {
        return u.setHost(NetUtils.getLocalHost()).toFullString();
      }
    } else if (host.contains(":")) {
      int i = host.lastIndexOf(':');
      if (NetUtils.isInvalidLocalHost(host.substring(0, i))) {
        return NetUtils.getLocalHost() + host.substring(i);
      }
    } else {
      if (NetUtils.isInvalidLocalHost(host)) {
        return NetUtils.getLocalHost();
      }
    }
    return host;
  }

  /**
   * 当前主机对应的InetAddress对象，用户缓存，一旦计算出来结果以后就直接使用
   */
  private static volatile InetAddress LOCAL_ADDRESS = null;

  /**
   * 遍历本地网卡，返回第一个合理的IP。
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static InetAddress getLocalAddress() {
    if (LOCAL_ADDRESS != null)
      return LOCAL_ADDRESS;
    InetAddress localAddress = getLocalAddress0();
    LOCAL_ADDRESS = localAddress;
    return localAddress;
  }

  /**
   * 获取日志的主机名，即当前主机名
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static String getLogHost() {
    InetAddress address = LOCAL_ADDRESS;
    return address == null ? LOCALHOST : address.getHostAddress();
  }

  /**
   * 获取当前主机的InetAddress对象
   *
   * @author sxp
   * @since 2018/12/1
   */
  private static InetAddress getLocalAddress0() {
    InetAddress localAddress = null;
    try {
      localAddress = InetAddress.getLocalHost();
      if (isValidAddress(localAddress)) {
        return localAddress;
      }
    } catch (Throwable e) {
      logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
    }
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      if (interfaces != null) {
        while (interfaces.hasMoreElements()) {
          try {
            NetworkInterface network = interfaces.nextElement();
            Enumeration<InetAddress> addresses = network.getInetAddresses();
            if (addresses != null) {
              while (addresses.hasMoreElements()) {
                try {
                  InetAddress address = addresses.nextElement();
                  if (isValidAddress(address)) {
                    return address;
                  }
                } catch (Throwable e) {
                  logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
                }
              }
            }
          } catch (Throwable e) {
            logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
          }
        }
      }
    } catch (Throwable e) {
      logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
    }
    logger.error("Could not get local host ip address, will use 127.0.0.1 instead.");
    return localAddress;
  }

  /**
   * 容量为1000的主机名缓存
   */
  private static final Map<String, String> hostNameCache = new LRUCache<String, String>(1000);

  /**
   * 获取主机名
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static String getHostName(String address) {
    try {
      int i = address.indexOf(':');
      if (i > -1) {
        address = address.substring(0, i);
      }
      String hostname = hostNameCache.get(address);
      if (hostname != null && hostname.length() > 0) {
        return hostname;
      }
      InetAddress inetAddress = InetAddress.getByName(address);
      if (inetAddress != null) {
        hostname = inetAddress.getHostName();
        hostNameCache.put(address, hostname);
        return hostname;
      }
    } catch (Throwable e) {
      // ignore
    }
    return address;
  }

  /**
   * 通过主机名获取IP
   *
   * @param hostName
   * @return ip address or hostName if UnknownHostException
   */
  public static String getIpByHost(String hostName) {
    try {
      return InetAddress.getByName(hostName).getHostAddress();
    } catch (UnknownHostException e) {
      return hostName;
    }
  }

  /**
   * 将InetSocketAddress对象转化为ip:port形式的字符串
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static String toAddressString(InetSocketAddress address) {
    return address.getAddress().getHostAddress() + ":" + address.getPort();
  }

  /**
   * 将字符串转化为InetSocketAddress对象
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static InetSocketAddress toAddress(String address) {
    int i = address.indexOf(':');
    String host;
    int port;
    if (i > -1) {
      host = address.substring(0, i);
      port = Integer.parseInt(address.substring(i + 1));
    } else {
      host = address;
      port = 0;
    }
    return new InetSocketAddress(host, port);
  }

  /**
   * 根据协议、主机名、端口、服务接口名称创建URL对象
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static String toURL(String protocol, String host, int port, String path) {
    StringBuilder sb = new StringBuilder();
    sb.append(protocol).append("://");
    sb.append(host).append(':').append(port);
    if (path.charAt(0) != '/')
      sb.append('/');
    sb.append(path);
    return sb.toString();
  }
}
