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


import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.model.RegistryCenter;
import com.orientsec.grpc.common.resource.AllRegisterCenterConf;
import com.orientsec.grpc.common.resource.SystemConfig;
import com.orientsec.grpc.common.util.MapUtils;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * URL工具类
 *
 * @author heiden
 * @since 2018/3/15.
 */
public class UrlUtils {
  private static Logger logger = LoggerFactory.getLogger(UrlUtils.class);

  /**
   * 将IP地址和指定的参数集合转化为URL
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static URL parseURL(String address, Map<String, String> defaults) {
    if (address == null || address.length() == 0) {
      return null;
    }
    String url;
    if (address.indexOf("://") >= 0) {
      url = address;
    } else {
      String[] addresses = Constants.COMMA_SPLIT_PATTERN.split(address);
      url = addresses[0];
      if (addresses.length > 1) {
        StringBuilder backup = new StringBuilder();
        for (int i = 1; i < addresses.length; i++) {
          if (i > 1) {
            backup.append(",");
          }
          backup.append(addresses[i]);
        }
        url += "?" + Constants.BACKUP_KEY + "=" + backup.toString();
      }
    }
    String defaultProtocol = defaults == null ? null : defaults.get("protocol");
    if (defaultProtocol == null || defaultProtocol.length() == 0) {
      defaultProtocol = "grpc";
    }
    String defaultUsername = defaults == null ? null : defaults.get("username");
    String defaultPassword = defaults == null ? null : defaults.get("password");
    int defaultPort = StringUtils.parseInteger(defaults == null ? null : defaults.get("port"));
    String defaultPath = defaults == null ? null : defaults.get("path");
    Map<String, String> defaultParameters = defaults == null ? null : new HashMap<String, String>(defaults);
    if (defaultParameters != null) {
      defaultParameters.remove("protocol");
      defaultParameters.remove("username");
      defaultParameters.remove("password");
      defaultParameters.remove("host");
      defaultParameters.remove("port");
      defaultParameters.remove("path");
    }
    URL u = URL.valueOf(url);
    boolean changed = false;
    String protocol = u.getProtocol();
    String username = u.getUsername();
    String password = u.getPassword();
    String host = u.getHost();
    int port = u.getPort();
    String path = u.getPath();
    Map<String, String> parameters = new HashMap<String, String>(u.getParameters());
    if ((protocol == null || protocol.length() == 0) && defaultProtocol != null && defaultProtocol.length() > 0) {
      changed = true;
      protocol = defaultProtocol;
    }
    if ((username == null || username.length() == 0) && defaultUsername != null && defaultUsername.length() > 0) {
      changed = true;
      username = defaultUsername;
    }
    if ((password == null || password.length() == 0) && defaultPassword != null && defaultPassword.length() > 0) {
      changed = true;
      password = defaultPassword;
    }
        /*if (u.isAnyHost() || u.isLocalHost()) {
            changed = true;
            host = NetUtils.getLocalHost();
        }*/
    if (port <= 0) {
      if (defaultPort > 0) {
        changed = true;
        port = defaultPort;
      } else {
        changed = true;
        port = 9090;
      }
    }
    if (path == null || path.length() == 0) {
      if (defaultPath != null && defaultPath.length() > 0) {
        changed = true;
        path = defaultPath;
      }
    }
    if (defaultParameters != null && defaultParameters.size() > 0) {
      for (Map.Entry<String, String> entry : defaultParameters.entrySet()) {
        String key = entry.getKey();
        String defaultValue = entry.getValue();
        if (defaultValue != null && defaultValue.length() > 0) {
          String value = parameters.get(key);
          if (value == null || value.length() == 0) {
            changed = true;
            parameters.put(key, defaultValue);
          }
        }
      }
    }
    if (changed) {
      u = new URL(protocol, username, password, host, port, path, parameters);
    }
    return u;
  }

  /**
   * 将IP地址集合和指定的参数集合转化为URL集合
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static List<URL> parseURLs(String address, Map<String, String> defaults) {
    if (address == null || address.length() == 0) {
      return null;
    }
    String[] addresses = Constants.REGISTRY_SPLIT_PATTERN.split(address);
    if (addresses == null || addresses.length == 0) {
      return null; //here won't be empty
    }
    List<URL> registries = new ArrayList<URL>();
    for (String addr : addresses) {
      registries.add(parseURL(addr, defaults));
    }
    return registries;
  }

  public static Map<String, Map<String, String>> convertRegister(Map<String, Map<String, String>> register) {
    Map<String, Map<String, String>> newRegister = new HashMap<String, Map<String, String>>();
    for (Map.Entry<String, Map<String, String>> entry : register.entrySet()) {
      String serviceName = entry.getKey();
      Map<String, String> serviceUrls = entry.getValue();
      if (!serviceName.contains(":") && !serviceName.contains("/")) {
        for (Map.Entry<String, String> entry2 : serviceUrls.entrySet()) {
          String serviceUrl = entry2.getKey();
          String serviceQuery = entry2.getValue();
          Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
          String group = params.get("group");
          String version = params.get("version");
          //params.remove("group");
          //params.remove("version");
          String name = serviceName;
          if (group != null && group.length() > 0) {
            name = group + "/" + name;
          }
          if (version != null && version.length() > 0) {
            name = name + ":" + version;
          }
          Map<String, String> newUrls = newRegister.get(name);
          if (newUrls == null) {
            newUrls = new HashMap<String, String>();
            newRegister.put(name, newUrls);
          }
          newUrls.put(serviceUrl, StringUtils.toQueryString(params));
        }
      } else {
        newRegister.put(serviceName, serviceUrls);
      }
    }
    return newRegister;
  }

  public static Map<String, String> convertSubscribe(Map<String, String> subscribe) {
    Map<String, String> newSubscribe = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : subscribe.entrySet()) {
      String serviceName = entry.getKey();
      String serviceQuery = entry.getValue();
      if (!serviceName.contains(":") && !serviceName.contains("/")) {
        Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
        String group = params.get("group");
        String version = params.get("version");
        //params.remove("group");
        //params.remove("version");
        String name = serviceName;
        if (group != null && group.length() > 0) {
          name = group + "/" + name;
        }
        if (version != null && version.length() > 0) {
          name = name + ":" + version;
        }
        newSubscribe.put(name, StringUtils.toQueryString(params));
      } else {
        newSubscribe.put(serviceName, serviceQuery);
      }
    }
    return newSubscribe;
  }

  public static Map<String, Map<String, String>> revertRegister(Map<String, Map<String, String>> register) {
    Map<String, Map<String, String>> newRegister = new HashMap<String, Map<String, String>>();
    for (Map.Entry<String, Map<String, String>> entry : register.entrySet()) {
      String serviceName = entry.getKey();
      Map<String, String> serviceUrls = entry.getValue();
      if (serviceName.contains(":") || serviceName.contains("/")) {
        for (Map.Entry<String, String> entry2 : serviceUrls.entrySet()) {
          String serviceUrl = entry2.getKey();
          String serviceQuery = entry2.getValue();
          Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
          String name = serviceName;
          int i = name.indexOf('/');
          if (i >= 0) {
            params.put("group", name.substring(0, i));
            name = name.substring(i + 1);
          }
          i = name.lastIndexOf(':');
          if (i >= 0) {
            params.put("version", name.substring(i + 1));
            name = name.substring(0, i);
          }
          Map<String, String> newUrls = newRegister.get(name);
          if (newUrls == null) {
            newUrls = new HashMap<String, String>();
            newRegister.put(name, newUrls);
          }
          newUrls.put(serviceUrl, StringUtils.toQueryString(params));
        }
      } else {
        newRegister.put(serviceName, serviceUrls);
      }
    }
    return newRegister;
  }

  public static Map<String, String> revertSubscribe(Map<String, String> subscribe) {
    Map<String, String> newSubscribe = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : subscribe.entrySet()) {
      String serviceName = entry.getKey();
      String serviceQuery = entry.getValue();
      if (serviceName.contains(":") || serviceName.contains("/")) {
        Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
        String name = serviceName;
        int i = name.indexOf('/');
        if (i >= 0) {
          params.put("group", name.substring(0, i));
          name = name.substring(i + 1);
        }
        i = name.lastIndexOf(':');
        if (i >= 0) {
          params.put("version", name.substring(i + 1));
          name = name.substring(0, i);
        }
        newSubscribe.put(name, StringUtils.toQueryString(params));
      } else {
        newSubscribe.put(serviceName, serviceQuery);
      }
    }
    return newSubscribe;
  }

  public static Map<String, Map<String, String>> revertNotify(Map<String, Map<String, String>> notify) {
    if (notify != null && notify.size() > 0) {
      Map<String, Map<String, String>> newNotify = new HashMap<String, Map<String, String>>();
      for (Map.Entry<String, Map<String, String>> entry : notify.entrySet()) {
        String serviceName = entry.getKey();
        Map<String, String> serviceUrls = entry.getValue();
        if (!serviceName.contains(":") && !serviceName.contains("/")) {
          if (serviceUrls != null && serviceUrls.size() > 0) {
            for (Map.Entry<String, String> entry2 : serviceUrls.entrySet()) {
              String url = entry2.getKey();
              String query = entry2.getValue();
              Map<String, String> params = StringUtils.parseQueryString(query);
              String group = params.get("group");
              String version = params.get("version");
              // params.remove("group");
              // params.remove("version");
              String name = serviceName;
              if (group != null && group.length() > 0) {
                name = group + "/" + name;
              }
              if (version != null && version.length() > 0) {
                name = name + ":" + version;
              }
              Map<String, String> newUrls = newNotify.get(name);
              if (newUrls == null) {
                newUrls = new HashMap<String, String>();
                newNotify.put(name, newUrls);
              }
              newUrls.put(url, StringUtils.toQueryString(params));
            }
          }
        } else {
          newNotify.put(serviceName, serviceUrls);
        }
      }
      return newNotify;
    }
    return notify;
  }

  public static List<String> revertForbid(List<String> forbid, Set<URL> subscribed) {
    if (forbid != null && forbid.size() > 0) {
      List<String> newForbid = new ArrayList<String>();
      for (String serviceName : forbid) {
        if (!serviceName.contains(":") && !serviceName.contains("/")) {
          for (URL url : subscribed) {
            if (serviceName.equals(url.getServiceInterface())) {
              newForbid.add(url.getServiceKey());
              break;
            }
          }
        } else {
          newForbid.add(serviceName);
        }
      }
      return newForbid;
    }
    return forbid;
  }

  public static URL getEmptyUrl(String service, String category) {
    String group = null;
    String version = null;
    int i = service.indexOf('/');
    if (i > 0) {
      group = service.substring(0, i);
      service = service.substring(i + 1);
    }
    i = service.lastIndexOf(':');
    if (i > 0) {
      version = service.substring(i + 1);
      service = service.substring(0, i);
    }
    return URL.valueOf(Constants.EMPTY_PROTOCOL + "://0.0.0.0/" + service + "?"
            + Constants.CATEGORY_KEY + "=" + category
            + (group == null ? "" : "&" + Constants.GROUP_KEY + "=" + group)
            + (version == null ? "" : "&" + Constants.VERSION_KEY + "=" + version));
  }

  public static boolean isMatchCategory(String category, String categories) {
    if (categories == null || categories.length() == 0) {
      return Constants.DEFAULT_CATEGORY.equals(category);
    } else if (categories.contains(Constants.ANY_VALUE)) {
      return true;
    } else if (categories.contains(Constants.REMOVE_VALUE_PREFIX)) {
      return !categories.contains(Constants.REMOVE_VALUE_PREFIX + category);
    } else {
      return categories.contains(category);
    }
  }

  /**
   * @since 2019-06-26 modify by sxp 为了实现对group属性的动态配置，去掉group的判断
   */
  public static boolean isMatch(URL consumerUrl, URL providerUrl) {
    String consumerInterface = consumerUrl.getServiceInterface();
    String providerInterface = providerUrl.getServiceInterface();
    if (!(Constants.ANY_VALUE.equals(consumerInterface) || StringUtils.isEquals(consumerInterface, providerInterface)))
      return false;

    if (!isMatchCategory(providerUrl.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY),
            consumerUrl.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY))) {
      return false;
    }
    if (!providerUrl.getParameter(Constants.ENABLED_KEY, true)
            && !Constants.ANY_VALUE.equals(consumerUrl.getParameter(Constants.ENABLED_KEY))) {
      return false;
    }

    String consumerVersion = consumerUrl.getParameter(Constants.VERSION_KEY);
    String consumerClassifier = consumerUrl.getParameter(Constants.CLASSIFIER_KEY, Constants.ANY_VALUE);

    String providerVersion = providerUrl.getParameter(Constants.VERSION_KEY);
    String providerClassifier = providerUrl.getParameter(Constants.CLASSIFIER_KEY, Constants.ANY_VALUE);
    return (consumerVersion == null || Constants.ANY_VALUE.equals(consumerVersion) || StringUtils.isEquals(consumerVersion, providerVersion))
            && (consumerClassifier == null || Constants.ANY_VALUE.equals(consumerClassifier) || StringUtils.isEquals(consumerClassifier, providerClassifier));
  }

  public static boolean isMatchGlobPattern(String pattern, String value, URL param) {
    if (param != null && pattern.startsWith("$")) {
      pattern = param.getRawParameter(pattern.substring(1));
    }
    return isMatchGlobPattern(pattern, value);
  }

  public static boolean isMatchGlobPattern(String pattern, String value) {
    if ("*".equals(pattern))
      return true;
    if ((pattern == null || pattern.length() == 0)
            && (value == null || value.length() == 0))
      return true;
    if ((pattern == null || pattern.length() == 0)
            || (value == null || value.length() == 0))
      return false;

    int i = pattern.lastIndexOf('*');
    // 没有找到星号
    if (i == -1) {
      return value.equals(pattern);
    }
    // 星号在末尾
    else if (i == pattern.length() - 1) {
      return value.startsWith(pattern.substring(0, i));
    }
    // 星号的开头
    else if (i == 0) {
      return value.endsWith(pattern.substring(i + 1));
    }
    // 星号的字符串的中间
    else {
      String prefix = pattern.substring(0, i);
      String suffix = pattern.substring(i + 1);
      return value.startsWith(prefix) && value.endsWith(suffix);
    }
  }

  public static boolean isServiceKeyMatch(URL pattern, URL value) {
    return pattern.getParameter(Constants.INTERFACE_KEY).equals(
            value.getParameter(Constants.INTERFACE_KEY))
            && isItemMatch(pattern.getParameter(Constants.GROUP_KEY),
            value.getParameter(Constants.GROUP_KEY))
            && isItemMatch(pattern.getParameter(Constants.VERSION_KEY),
            value.getParameter(Constants.VERSION_KEY));
  }

  /**
   * 判断 value 是否匹配 pattern，pattern 支持 * 通配符.
   *
   * @param pattern pattern
   * @param value   value
   * @return true if match otherwise false
   */
  static boolean isItemMatch(String pattern, String value) {
    if (pattern == null) {
      return value == null;
    } else {
      return "*".equals(pattern) || pattern.equals(value);
    }
  }

  /**
   * 获取注册中心对应的URL
   *
   * @since 2019/9/10 方法重命名，并将可复用代码提取未一个独立方法
   */
  public static URL getRegisterURL(String key) {
    Properties properties = SystemConfig.getProperties();
    if (properties == null) {
      logger.error("读取配置文件错误，无法获取配置信息");
      return null;
    }

    String addresses = properties.getProperty(key);
    if (StringUtils.isEmpty(addresses)) {
      logger.warn("配置文件中没有读取到注册中心的服务器列表信息");
      return null;
    }

    URL url = getZkUrlByAddress(addresses, key, null, null);

    return url;
  }


  /**
   * 将注册中心地址转化为URL
   *
   * @param addresses 注册中心服务器地址列表
   * @param id        给该注册中心地址设置一个唯一标识
   * @since 2019/12/03 modify by wlh 增加封装配置的ip和port数据到map
   */
  public static URL getZkUrlByAddress(String addresses, String id, String serviceIp, Integer servicePort) {
    if (StringUtils.isEmpty(addresses)) {
      return null;
    }

    Map<String, String> parameters;
    parameters = new HashMap<>(MapUtils.capacity(4));
    if (StringUtils.isNotEmpty(serviceIp)) {
      parameters.put(GlobalConstants.CommonKey.SERVICE_IP, serviceIp);
    }

    if (servicePort != null) {
      parameters.put(GlobalConstants.CommonKey.SERVICE_PORT, String.valueOf(servicePort));
    }

    if (StringUtils.isNotEmpty(id)) {
      parameters.put("id", id);
    }

    String address;
    int index = addresses.indexOf(",");

    if (index < 0) {
      address = addresses;
    } else {
      address = addresses.substring(0, index);
      String backupAddr = addresses.substring(index + 1);
      parameters.put(Constants.BACKUP_KEY, backupAddr);
    }

    String[] ipAndPort = address.split(":");
    String registryIp;
    int registryPort = GlobalConstants.Zookeeper.DEFAULT_PORT;
    if (ipAndPort.length == 2) {
      registryIp = ipAndPort[0];
      registryPort = Integer.valueOf(ipAndPort[1]);
    } else {
      registryIp = ipAndPort[0];
    }

    URL url = new URL(GlobalConstants.Zookeeper.PROTOCOL_PREFIX, registryIp, registryPort, parameters);

    return url;
  }


  /**
   * 服务端注册的所有注册中心对应的URL
   *
   * @author yulei
   * @since 2019/8/27
   * @since 2019/11/29 modify by wlh 增加注册中心serviceIP和servicePort属性到url的Parameter中
   */
  public static List<URL> getAllProviderRegisterURLs() {
    Properties properties = SystemConfig.getProperties();
    if (properties == null) {
      logger.error("读取配置文件错误，无法获取配置信息");
      return null;
    }

    ConcurrentMap<String, RegistryCenter> allConfMap = AllRegisterCenterConf.getAllConfMap();
    List<URL> urlList = new ArrayList<>(allConfMap.size());

    String addresses, addrKey;
    URL url;

    for (Map.Entry<String, RegistryCenter> entry : allConfMap.entrySet()) {
      addrKey = entry.getKey();
      addresses = properties.getProperty(addrKey);
      if (StringUtils.isEmpty(addresses)) {
        continue;
      }

      RegistryCenter registryCenter = entry.getValue();
      url = getZkUrlByAddress(addresses, addrKey, registryCenter.getServiceIp(), registryCenter.getServicePort());

      if (url != null) {
        urlList.add(url);
      }
    }

    return urlList;
  }

}
