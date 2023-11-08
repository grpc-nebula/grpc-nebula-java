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


import com.orientsec.grpc.common.exception.BusinessException;
import com.orientsec.grpc.common.resource.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import static com.orientsec.grpc.common.constant.GlobalConstants.COMMON_LOCALHOST_IP;

/**
 * IP工具类
 *
 * @author sxp
 * @since V1.0 2017-3-27
 */
public final class IpUtils {
  private static final Logger logger = LoggerFactory.getLogger(IpUtils.class);

  public static final String LOCALHOST = "127.0.0.1";
  public static final String ANYHOST = "0.0.0.0";

  /**
   * IPv4地址的正则表达式匹配模式
   * <p>
   * xxx.xxx.xxx.xxx <br>
   * xxx的取值范围是:0~255
   * </p>
   */
  private static final Pattern IP_PATTERN = Pattern
          .compile("((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)");

  private static final String LOCALHOST_IP = initLocalhostIpFromConfig();

  /**
   * 从配置文件里读取当前服务器的IP地址
   * <p>
   * 使用场合：一台服务器安装有多个网卡的情况
   * </p>
   *
   * @author sxp
   * @since 2019/2/21
   */
  private static String initLocalhostIpFromConfig() {
    Properties properties = SystemConfig.getProperties();
    String ip = PropertiesUtils.getStringValue(properties, COMMON_LOCALHOST_IP, null);

    if (ip != null) {
      ip = ip.trim();
    }

    if (StringUtils.isNotEmpty(ip) && !isValidIpv4Intenal(ip)) {
      ip = null;
    }

    if (StringUtils.isNotEmpty(ip)) {
      List<InetAddress> list = getIP4ByNetworkInterface();

      if (!list.isEmpty()) {
        List<String> validIps = new ArrayList<>(list.size());

        for (InetAddress address : list) {
          if (address != null && address instanceof Inet4Address) {
            if (isValidAddress(address)) {
              validIps.add(address.getHostAddress());
            }
          }
        }

        // 检查配置的IP是否在网卡中
        if (!validIps.contains(ip)) {
          logger.warn("配置文件中指定的服务注册IP地址[" + ip + "]，不属于本机网卡中的IP地址，忽略该配置！");
          ip = null;
        }
      }
    }

    if (StringUtils.isNotEmpty(ip)) {
      logger.info(COMMON_LOCALHOST_IP + "=" + ip);
    }

    return ip;
  }

  /**
   * 获取本机IP
   * <p>
   * 优先使用InetAddress获取IP，获取不到再使用NetworkInterface <br>
   * </p>
   *
   * @author sxp
   * @since V1.0 2017-3-27
   * @since V1.1 2017-4-11 直接调用getLocalHostAddress方法
   */
  public static String getIP4WithPriority() {
    return getLocalHostAddress();
  }

  /**
   * 获取当前主机的IP地址
   * <p>
   * 参考：com.alibaba.dubbo.common.utils.NetUtils#getLocalAddress <br>
   * 优先使用InetAddress获取IP，获取不到再使用NetworkInterface <br>
   * </p>
   *
   * @return 如果获取不到有效的InetAddress对象，返回127.0.0.1
   * @author sxp
   * @since V1.0 2017-4-11
   */
  public static String getLocalHostAddress() {
    if (StringUtils.isNotEmpty(LOCALHOST_IP)) {
      return LOCALHOST_IP;
    }

    InetAddress address = getLocalAddress();
    return (address == null) ? LOCALHOST : address.getHostAddress();
  }

  private static volatile InetAddress LOCAL_ADDRESS = null;

  private static InetAddress getLocalAddress() {
    if (LOCAL_ADDRESS != null) {
      return LOCAL_ADDRESS;
    }
    InetAddress localAddress = getLocalAddress0();
    LOCAL_ADDRESS = localAddress;
    return localAddress;
  }

  private static InetAddress getLocalAddress0() {
    InetAddress localAddress = null;

    try {
      localAddress = InetAddress.getLocalHost();
      if (isValidAddress(localAddress)) {
        return localAddress;
      }
    } catch (UnknownHostException e) {
      //logger.logger(Level.SEVERE, "获取InetAddress.getLocalHost()失败, " + e.getMessage(), e);
    }

    List<InetAddress> list = IpUtils.getIP4ByNetworkInterface();
    for (InetAddress address : list) {
      if (address != null && address instanceof Inet4Address) {
        if (isValidAddress(address)) {
          return address;
        }
      }
    }

    return localAddress;
  }

  private static boolean isValidAddress(InetAddress address) {
    if (address == null || address.isLoopbackAddress()) {
      return false;
    }
    String ip = address.getHostAddress();
    if (ip == null) {
      return false;
    }
    return (!ANYHOST.equals(ip) && !LOCALHOST.equals(ip) && isValidIPv4(ip));
  }

  private static boolean isValidIpv4Intenal(String ip) {
    if (StringUtils.isEmpty(ip)) {
      return false;
    }
    return (!ANYHOST.equals(ip) && !LOCALHOST.equals(ip) && isValidIPv4(ip));
  }



  /**
   * 检验IPv4地址的合法性
   *
   * @author sxp
   * @since V1.0 Apr 11, 2017
   */
  public static boolean isValidIPv4(String ip) {
    if (StringUtils.isEmpty(ip)) {
      return false;
    }
    return IP_PATTERN.matcher(ip).matches();
  }

  /**
   * 端口合法性校验
   *
   * @param port
   * @return
   * @author wlh
   * @since 2019/12/06
   */
  public static boolean isValidPort(String port){
    if (StringUtils.isEmpty(port)) {
      return false;
    }

    if (MathUtils.isInteger(port)) {
      int portInt = Integer.parseInt(port);
      if (portInt >= 0 && portInt <= 65535) {
        return true;
      }
    }

    return false;
  }

  /**
   * 获取本机的网络接口的详细信息
   *
   * @author sxp
   * @since V1.0 2016-8-24
   */
  public static List<InetAddress> getIP4ByNetworkInterface() {
    List<InetAddress> ipList = new ArrayList<InetAddress>();

    try {
      Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();

      if (allNetInterfaces == null) {
        logger.error("获取NetworkInterface.getNetworkInterfaces失败!");
        return ipList;
      }

      InetAddress address;
      Enumeration<InetAddress> addresses;
      NetworkInterface netInterface;

      while (allNetInterfaces.hasMoreElements()) {
        netInterface = allNetInterfaces.nextElement();
        addresses = netInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          address = addresses.nextElement();
          // 只获取IPv4
          if (address != null && address instanceof Inet4Address) {
            ipList.add(address);
          }
        }
      }
    } catch (SocketException e) {
      logger.error(e.getMessage(), e);
    }

    return ipList;
  }

  /**
   * 是否为内网IP地址(是否为本地局域网IP地址)
   *
   * <pre>
   * 内网保留地址：
   * A类  10.0.0.0-10.255.255.255
   * B类  172.16.0.0-172.31.255.255
   * C类  192.168.0.0-192.168.255.255
   * 特例 127.0.0.1
   * </pre>
   *
   * @author sxp
   * @since V1.0 2016-9-2
   */
  public static boolean isInnerIP(String ipAddress) {
    boolean isInnerIp = false;
    long ipNum = getIpNum(ipAddress);

    long aBegin = getIpNum("10.0.0.0");
    long aEnd = getIpNum("10.255.255.255");

    long bBegin = getIpNum("172.16.0.0");
    long bEnd = getIpNum("172.31.255.255");

    long cBegin = getIpNum("192.168.0.0");
    long cEnd = getIpNum("192.168.255.255");

    isInnerIp = isInner(ipNum, aBegin, aEnd) || isInner(ipNum, bBegin, bEnd) || isInner(ipNum, cBegin, cEnd)
            || "127.0.0.1".equals(ipAddress);
    return isInnerIp;
  }

  private static final long IP_MULTIPLE_A = 256L * 256 * 256;
  private static final long IP_MULTIPLE_B = 256L * 256;
  private static final long IP_MULTIPLE_C = 256L;

  /**
   * 将IP地址转换为整数
   */
  public static long getIpNum(String ipAddress) {
    if (!isValidIPv4(ipAddress)) {
      throw new BusinessException("IpUtils.getIpNum:传入的IP地址[" + ipAddress + "]无效，请检查！");
    }

    String[] ip = ipAddress.split("\\.");
    long a = Integer.parseInt(ip[0]);
    long b = Integer.parseInt(ip[1]);
    long c = Integer.parseInt(ip[2]);
    long d = Integer.parseInt(ip[3]);

    long ipNum = a * IP_MULTIPLE_A + b * IP_MULTIPLE_B + c * IP_MULTIPLE_C + d;
    return ipNum;
  }

  private static boolean isInner(long userIp, long begin, long end) {
    return (userIp >= begin) && (userIp <= end);
  }

  /**
   * 检查IP是否能够连通
   *
   * @author sxp
   * @since V1.0 Apr 14, 2017
   */
  public static boolean isReachable(String ip) {
    if (!isValidIPv4(ip)) {
      return false;
    }

    boolean isReachable;

    try {
      InetAddress address = InetAddress.getByName(ip);
      int timeout = 2000;// 2秒超时时间

      logger.info("正在测试IP地址[" + ip + "]的连通性...");

      isReachable = address.isReachable(timeout);

    } catch (Exception e) {
      isReachable = false;
      logger.info("IP地址[" + ip + "]无法联通", e);
    }

    return isReachable;
  }



}
