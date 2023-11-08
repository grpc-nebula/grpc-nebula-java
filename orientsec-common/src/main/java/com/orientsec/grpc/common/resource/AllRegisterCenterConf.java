package com.orientsec.grpc.common.resource;

import com.orientsec.grpc.common.model.RegistryCenter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 所有注册中心配置信息
 *
 * @author Shawpin Shi
 * @since 2019/9/29
 */
public class AllRegisterCenterConf {
  private static volatile ConcurrentMap<String, RegistryCenter> allConfMap = null;

  /**
   * 获得所有注册中心
   * <p>
   * 公共注册中心、私有注册中心、服务端额外注册的注册中心
   * </p>
   *
   * @author Shawpin Shi
   * @since 2019/9/29
   */
  public static ConcurrentMap<String, RegistryCenter> getAllConfMap() {
    if (allConfMap == null) {
      initAllConfMap();
    }

    return allConfMap;
  }

  private synchronized static void initAllConfMap() {
    allConfMap = new ConcurrentHashMap<>();
    allConfMap.putAll(RegisterCenterConf.getRcConfMap());
    allConfMap.putAll(PrivateRegisterCenterConf.getPrivateRcConfMap());
    allConfMap.putAll(ProviderRegisterCenterConf.getExtraConfMap());
  }
}
