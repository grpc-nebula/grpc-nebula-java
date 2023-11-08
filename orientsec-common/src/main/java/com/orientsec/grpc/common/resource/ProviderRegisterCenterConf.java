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

package com.orientsec.grpc.common.resource;

import com.orientsec.grpc.common.model.RegistryCenter;
import com.orientsec.grpc.common.util.MathUtils;
import com.orientsec.grpc.common.util.StringUtils;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.orientsec.grpc.common.constant.GlobalConstants.*;
import static com.orientsec.grpc.common.resource.RegisterCenterConf.formatRootPath;
import static com.orientsec.grpc.common.resource.RegisterCenterConf.formatUserPwd;

/**
 * 服务端注册中心配置信息
 *
 * @author yulei
 * @since 2019/9/2
 * @since 2019/9/10 调整类名、变量名、方法名，简化代码
 */
public class ProviderRegisterCenterConf {
  private static String PREFIX = SERVICE_EXTRA_RC_PREFIX;

  /**
   * 正则表达式，匹配zookeeper.service-register-xxx.host.server类型的字符串
   */
  private static String PROP_KEY_REGEX = PREFIX + "\\d+" + SERVICE_EXTRA_RC_ADDR_SUFFIX;
  private static Pattern PROP_KEY_PATTERN = Pattern.compile(PROP_KEY_REGEX);



  // key值:注册中心地址参数名称(例如zookeeper.service-register-01.host.server,...)
  private static ConcurrentMap<String, RegistryCenter> extraConfMap = new ConcurrentHashMap<>();

  static {
    initExtraConfMap();
  }


  /**
   * 初始化服务端额外使用的注册中心配置
   */
  private static void initExtraConfMap() {
    Properties properties = SystemConfig.getProperties();
    if (properties == null) {
      return;
    }

    String host, rootPath, aclUser, aclPwd, serviceIp, servicePort;
    RegistryCenter registryCenter;

    for (String propKey : properties.stringPropertyNames()) {
      if (propKey.startsWith(PREFIX) &&
              propKey.endsWith(SERVICE_EXTRA_RC_ADDR_SUFFIX)) {
        String[] zkPropKeys = getExtraRegistryCenterKeys(propKey);
        if (zkPropKeys == null) {
          continue;
        }

        host = properties.getProperty(zkPropKeys[0]);
        rootPath = formatRootPath(properties.getProperty(zkPropKeys[1]));
        aclUser = properties.getProperty(zkPropKeys[2]);
        aclPwd = properties.getProperty(zkPropKeys[3]);
        serviceIp = properties.getProperty(zkPropKeys[4]);
        servicePort = properties.getProperty(zkPropKeys[5]);

        registryCenter = new RegistryCenter();
        registryCenter.setHost(host);
        registryCenter.setRootPath(rootPath);
        registryCenter.setAclUserPwd(formatUserPwd(aclUser, aclPwd));
        registryCenter.setServiceIp(serviceIp);

        if (StringUtils.isEmpty(servicePort)) {
          registryCenter.setServicePort(null);
        } else {
          if (MathUtils.isInteger(servicePort)) {
            registryCenter.setServicePort(Integer.parseInt(servicePort));
          } else {
            registryCenter.setServicePort(ERROR_PORT);
          }
        }
        extraConfMap.put(propKey, registryCenter);
      }
    }
  }




  /**
   * 组装服务额外注注册中心键值，例如
   * zookeeper.service-register-01.host.server
   * zookeeper.service-register-01.root
   * zookeeper.service-register-01.acl.username
   * zookeeper.service-register-01.acl.password
   * zookeeper.service-register-01.service.ip
   * zookeeper.service-register-01.service.port
   *
   * @author yulei
   * @since 2019/9/2
   */
  private static String[] getExtraRegistryCenterKeys(String propKey) {
    Matcher m = PROP_KEY_PATTERN.matcher(propKey);
    if (!m.matches()) {
      return null;
    }

    int pos1 = PREFIX.length();
    int pos2 = propKey.indexOf(SERVICE_EXTRA_RC_ADDR_SUFFIX);
    String numberStr = propKey.substring(pos1, pos2);

    String rootPathKey = PREFIX + numberStr + SERVICE_EXTRA_RC_ROOTPATH_SUFFIX;
    String aclUserKey = PREFIX + numberStr + SERVICE_EXTRA_RC_ACLUSER_SUFFIX;
    String aclPwdKey = PREFIX + numberStr + SERVICE_EXTRA_RC_ACLPWD_SUFFIX;
    String serviceIp = PREFIX + numberStr + SERVICE_EXTRA_RC_SERVICEIP_SUFFIX;
    String servicePort = PREFIX + numberStr + SERVICE_EXTRA_RC_SERVICEPORT_SUFFIX;

    String[] zkPropKeys = new String[6];
    zkPropKeys[0] = propKey;
    zkPropKeys[1] = rootPathKey;
    zkPropKeys[2] = aclUserKey;
    zkPropKeys[3] = aclPwdKey;
    zkPropKeys[4] = serviceIp;
    zkPropKeys[5] = servicePort;

    return zkPropKeys;
  }

  public static ConcurrentMap<String, RegistryCenter> getExtraConfMap() {
    return extraConfMap;
  }
}
