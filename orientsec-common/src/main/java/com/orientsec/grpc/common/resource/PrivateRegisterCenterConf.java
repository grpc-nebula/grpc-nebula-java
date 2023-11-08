package com.orientsec.grpc.common.resource;

import com.orientsec.grpc.common.collect.ConcurrentHashSet;
import com.orientsec.grpc.common.model.RegistryCenter;
import com.orientsec.grpc.common.util.StringUtils;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.orientsec.grpc.common.constant.GlobalConstants.PRIVATE_REGISTRY_CENTER_ACLPWD;
import static com.orientsec.grpc.common.constant.GlobalConstants.PRIVATE_REGISTRY_CENTER_ACLUSER;
import static com.orientsec.grpc.common.constant.GlobalConstants.PRIVATE_REGISTRY_CENTER_ADDRESS;
import static com.orientsec.grpc.common.constant.GlobalConstants.PRIVATE_REGISTRY_CENTER_ROOT;
import static com.orientsec.grpc.common.constant.GlobalConstants.PRIVATE_SERVICE_LIST;
import static com.orientsec.grpc.common.resource.RegisterCenterConf.formatRootPath;
import static com.orientsec.grpc.common.resource.RegisterCenterConf.formatUserPwd;

/**
 * 私有注册中心配置信息
 *
 * @author yulei
 * @since 2019/9/19
 */
public class PrivateRegisterCenterConf {
  // key值:zookeeper.private.host.server
  private static ConcurrentMap<String, RegistryCenter> privateRcConfMap = new ConcurrentHashMap<>();

  // 私有服务名称列表
  private static ConcurrentHashSet<String> privateServices = new ConcurrentHashSet<>();

  private static Properties properties;

  static {
    properties = SystemConfig.getProperties();
    initPrivateRcConfMap();
    initPrivateServiceList();
  }

  /**
   * 初始化私有注册中心
   */
  private static void initPrivateRcConfMap() {
    if (properties == null) {
      return;
    }

    String host = properties.getProperty(PRIVATE_REGISTRY_CENTER_ADDRESS);
    if(StringUtils.isEmpty(host)){
      return;
    }
    String rootPath = properties.getProperty(PRIVATE_REGISTRY_CENTER_ROOT);
    String aclUser = properties.getProperty(PRIVATE_REGISTRY_CENTER_ACLUSER);
    String aclPwd = properties.getProperty(PRIVATE_REGISTRY_CENTER_ACLPWD);

    RegistryCenter registryCenter = new RegistryCenter();

    registryCenter.setHost(host);
    registryCenter.setRootPath(formatRootPath(rootPath));
    registryCenter.setAclUserPwd(formatUserPwd(aclUser, aclPwd));
    privateRcConfMap.put(PRIVATE_REGISTRY_CENTER_ADDRESS, registryCenter);
  }

  /**
   * 初始化私有服务名称列表
   */
  private static void initPrivateServiceList(){
    if (properties == null) {
      return;
    }
    String privateServicesStr = properties.getProperty(PRIVATE_SERVICE_LIST, null);

    if(StringUtils.isNotEmpty(privateServicesStr)){
      String[] services = privateServicesStr.split(",");

      for (String s : services) {
        s = StringUtils.trim(s);
        if (StringUtils.isNotEmpty(s)) {
          privateServices.add(s);
        }
      }
    }
  }

  public static ConcurrentMap<String, RegistryCenter> getPrivateRcConfMap() {
    return privateRcConfMap;
  }

  public static ConcurrentHashSet<String> getPrivateServices(){
    return privateServices;
  }

}
