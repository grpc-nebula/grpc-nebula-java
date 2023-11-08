package com.orientsec.grpc.common.resource;

import com.orientsec.grpc.common.collect.ConcurrentHashSet;
import com.orientsec.grpc.common.model.RegistryCenter;
import com.orientsec.grpc.common.util.DesEncryptUtils;
import com.orientsec.grpc.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.orientsec.grpc.common.constant.GlobalConstants.ACL_PASSWORD;
import static com.orientsec.grpc.common.constant.GlobalConstants.ACL_USERNAME;
import static com.orientsec.grpc.common.constant.GlobalConstants.COMMON_ROOT;
import static com.orientsec.grpc.common.constant.GlobalConstants.DEFAULT_ROOTPATH;
import static com.orientsec.grpc.common.constant.GlobalConstants.PRIVATE_REGISTRY_CENTER_ADDRESS;
import static com.orientsec.grpc.common.constant.GlobalConstants.PUBLIC_PRIVATE_REGISTRY_CENTER;
import static com.orientsec.grpc.common.constant.GlobalConstants.PUBLIC_SERVICE_LIST;
import static com.orientsec.grpc.common.constant.GlobalConstants.REGISTRY_CENTTER_ADDRESS;

/**
 * 注册中心配置信息
 *
 * @author sxp
 * @since 2019/9/10
 */
public class RegisterCenterConf {
  private static final Logger logger = LoggerFactory.getLogger(RegisterCenterConf.class);

  // key值:zookeeper.host.server
  private static ConcurrentMap<String, RegistryCenter> rcConfMap = new ConcurrentHashMap<>();

  private static ConcurrentMap<String, RegistryCenter> consumerRcConfMap = new ConcurrentHashMap<>();

  // 公共服务名称列表
  private static ConcurrentHashSet<String> publicServices = new ConcurrentHashSet<>();

  /**
   *  配置文件中非空注册中心的key值
   *  <p>
   *  如果公有、私有注册中心同时存在，取公有注册中心的key值
   *  </p>
   */
  private static String rcProKey;

  private static String consumerRcProKey;

  private static Properties properties;

  static {
    properties = SystemConfig.getProperties();
    initRcConfMap();
    intiPublicServiceList();
    initRcProKey();
    initConsumerRcProKey();
  }

  private static void initRcConfMap() {
    if (properties == null) {
      return;
    }

    String host = properties.getProperty(REGISTRY_CENTTER_ADDRESS);
    String rootPath = properties.getProperty(COMMON_ROOT);
    String aclUser = properties.getProperty(ACL_USERNAME);
    String aclPwd = properties.getProperty(ACL_PASSWORD);

    RegistryCenter registryCenter = new RegistryCenter();

    registryCenter.setHost(host);
    registryCenter.setRootPath(formatRootPath(rootPath));
    registryCenter.setAclUserPwd(formatUserPwd(aclUser, aclPwd));
    rcConfMap.put(REGISTRY_CENTTER_ADDRESS, registryCenter);
  }


  private static void intiPublicServiceList() {
    if (properties == null) {
      return;
    }
    String publicServicesStr = properties.getProperty(PUBLIC_SERVICE_LIST, null);

    if (StringUtils.isNotEmpty(publicServicesStr)) {
      String[] services = publicServicesStr.split(",");

      for (String s : services) {
        s = StringUtils.trim(s);
        if (StringUtils.isNotEmpty(s)) {
          publicServices.add(s);
        }
      }
    }
  }

  /**
   * 初始化公有注册中心、私有注册中心配置文件的key
   */
  private static void initRcProKey() {
    if (properties == null) {
      return;
    }
    String publicHost = properties.getProperty(REGISTRY_CENTTER_ADDRESS);
    String privateHost = properties.getProperty(PRIVATE_REGISTRY_CENTER_ADDRESS);
    if (StringUtils.isEmpty(publicHost) && StringUtils.isEmpty(privateHost)) {
      logger.error("配置出错，未配置注册中心服务器列表");
      return;
    }

    // 取注册中心的key值。如果公有、私有同时存在，取公有注册中心。
    if (StringUtils.isEmpty(publicHost)) {
      rcProKey = PRIVATE_REGISTRY_CENTER_ADDRESS;
    } else {
      rcProKey = REGISTRY_CENTTER_ADDRESS;
    }
  }

  private static void initConsumerRcProKey() {
    if (properties == null) {
      return;
    }
    String publicHost = properties.getProperty(REGISTRY_CENTTER_ADDRESS);
    String privateHost = properties.getProperty(PRIVATE_REGISTRY_CENTER_ADDRESS);
    if (StringUtils.isEmpty(publicHost) && StringUtils.isEmpty(privateHost)) {
      logger.error("配置出错，未配置注册中心服务器列表");
      return;
    }

    if(StringUtils.isNotEmpty(publicHost) && StringUtils.isNotEmpty(privateHost)){
      consumerRcProKey = PUBLIC_PRIVATE_REGISTRY_CENTER;
    }else if (StringUtils.isNotEmpty(publicHost)) {
      //只配置了public
      consumerRcProKey = REGISTRY_CENTTER_ADDRESS;
    } else {
      //只配置了private
      consumerRcProKey = PRIVATE_REGISTRY_CENTER_ADDRESS;
    }
  }

  /**
   * 格式化注册根路径，去空，取默认值
   */
  static String formatRootPath(String rootPath) {
    // 默认值
    if (StringUtils.isEmpty(rootPath)) {
      rootPath = DEFAULT_ROOTPATH;
    } else {
      rootPath = rootPath.trim();
    }

    if (rootPath.endsWith("/")) {
      rootPath = rootPath.substring(0, rootPath.length() - 1);
    }

    return rootPath;
  }


  static String formatUserPwd(String aclUser, String aclPwd) {
    if (StringUtils.isNotEmpty(aclUser) && StringUtils.isNotEmpty(aclPwd)) {
      aclPwd = decryptPassword(aclPwd);
      if (StringUtils.isEmpty(aclPwd)) {
        return null;
      } else {
        return aclUser + ":" + aclPwd;
      }
    } else {
      return null;
    }
  }

  static String decryptPassword(String password) {
    try {
      password = DesEncryptUtils.decrypt(password);
    } catch (Exception e) {
      logger.error("访问控制密码解密失败", e);
      return null;
    }
    return password;
  }


  public static ConcurrentMap<String, RegistryCenter> getRcConfMap() {
    return rcConfMap;
  }

  public static ConcurrentHashSet<String> getPublicServices() {
    return publicServices;
  }

  public static String getRcProKey() {
    return rcProKey;
  }

  public static String getConsumerRcProKey(){
    return consumerRcProKey;
  }

  public static ConcurrentMap<String, RegistryCenter> getConsumerRcConfMap(){
    consumerRcConfMap.putAll(getRcConfMap());
    consumerRcConfMap.putAll(PrivateRegisterCenterConf.getPrivateRcConfMap());
    return consumerRcConfMap;
  }
}
