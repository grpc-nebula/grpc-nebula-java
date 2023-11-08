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
package com.orientsec.grpc.provider.other;

import com.google.common.base.Preconditions;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.common.exception.BusinessException;
import com.orientsec.grpc.common.model.BusinessResult;
import com.orientsec.grpc.common.model.ConfigFile;
import com.orientsec.grpc.common.resource.SystemConfig;
import com.orientsec.grpc.common.util.ConfigFileHelper;
import com.orientsec.grpc.common.util.IpUtils;
import com.orientsec.grpc.common.util.ProcessUtils;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.provider.common.ProviderConstants;
import com.orientsec.grpc.provider.core.ProviderServiceRegistry;
import com.orientsec.grpc.provider.core.ServiceConfigUtils;
import com.orientsec.grpc.provider.watch.ProvidersListener;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.service.Provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * 测试用例专用的服务注册
 * <p>
 * 目的：为了使测试用例能运行通过
 * <p/>
 *
 * @author sxp
 * @since V1.0 2017/4/1
 */
public class Registry4TestCase implements ProviderServiceRegistry {
  //private static final Logger logger = LoggerFactory.getLogger(Registry4TestCase.class);
  private List<Map<String, Object>> servicesConfig;
  private List<Map<String, Object>> listenersInfo;
  private Map<String, Object> providerConfig = new HashMap<String, Object>(ProviderConstants.CONFIG_CAPACITY);
  private List<String> interfaceNames;
  private List<Map<String, Object>> servicesParams;
  private String ip;

  /**
   * 注册
   *
   * @param servicesParams 服务的属性
   * @author sxp
   * @since 2018/12/1
   */
  @Override
  public void register(List<Map<String, Object>> servicesParams) throws BusinessException {
    this.servicesParams = servicesParams;
    servicesConfig = new ArrayList<Map<String, Object>>(servicesParams.size());
    listenersInfo = new ArrayList<Map<String, Object>>(servicesParams.size());

    ip = IpUtils.getIP4WithPriority();

    BusinessResult result = getInterfaceNames();
    if (!result.isSuccess()) {
      throw new BusinessException("注册服务失败:" + result.getMessage());
    }

    /**
     * 读取服务配置文件
     */
    Properties pros = SystemConfig.getProperties();
    if (pros == null) {
      throw new BusinessException("注册服务[" + interfaceNames + "]失败:读取配置文件失败，请确认属性文件的路径["
              + GlobalConstants.CONFIG_FILE_PATH + "]是否正确！");
    }

    /**
     * 校验和保存配置文件信息
     */
    result = checkAndSaveProperties(pros);
    if (!result.isSuccess()) {
      throw new BusinessException("注册服务[" + interfaceNames + "]失败:" + result.getMessage());
    }

    /**
     * 保存当前服务器上所有服务的配置信息
     */
    saveServicesConfig();

    /**
     * 注册并增加监听器
     */
    try {
      doRegister();
    } catch (Exception e) {
      throw new BusinessException(e);
    }
  }


  /**
   * 测试用例停止后zk会将临时节点自动清理掉，所以该方法不需要实现
   *
   * @param servicesParams 服务的属性
   * @author sxp
   * @since 2018/12/1
   */
  @Override
  public void unRegister(List<Map<String, Object>> servicesParams) throws BusinessException {
  }

  /**
   * 获取服务接口名称
   *
   * @author sxp
   * @since 2018/12/1
   */
  private BusinessResult getInterfaceNames() {
    String key = GlobalConstants.Provider.Key.INTERFACE;
    String value, msg;

    interfaceNames = new ArrayList<String>(servicesParams.size());

    for (Map<String, Object> map : servicesParams) {
      if (map.containsKey(key)) {
        value = (String) map.get(key);
        interfaceNames.add(value);
      } else {
        msg = "传入的参数中servicesParams缺少" + key + "信息！";
        return new BusinessResult(false, msg);
      }
    }

    BusinessResult result = new BusinessResult(true, null);
    return result;
  }

  /**
   * 检查参数配置、保存参数配置
   *
   * @author sxp
   * @since 2018/12/1
   */
  private BusinessResult checkAndSaveProperties(Properties pros) {
    String msg;

    if (pros == null || pros.isEmpty()) {
      msg = "配置文件[" + GlobalConstants.CONFIG_FILE_PATH + "]中未配置有效属性！";
      return new BusinessResult(false, msg);
    }

    List<ConfigFile> allConf = ConfigFileHelper.getProvider();
    String proName, value;

    for (ConfigFile conf : allConf) {
      if (ConfigFileHelper.confFileCommonKeys.containsKey(conf.getName())) {
        proName = ConfigFileHelper.COMMON_KEY_PREFIX + conf.getName();// 配置文件中的属性名
      } else {
        proName = ConfigFileHelper.PROVIDER_KEY_PREFIX + conf.getName();
      }

      if (pros.containsKey(proName)) {
        value = pros.getProperty(proName);
        if (value != null) {
          value = value.trim();// 去空格
        }

        providerConfig.put(conf.getName(), value);
      } else {
        value = null;

        providerConfig.put(conf.getName(), conf.getDefaultValue());
      }

      /**
       * 校验必填项
       */
      if (conf.isRequired() && StringUtils.isEmpty(value)) {
        msg = "配置文件[" + GlobalConstants.CONFIG_FILE_PATH + "]中[" + proName + "]是必填项，属性值不能为空！";
        return new BusinessResult(false, msg);
      }
    }

    // 服务上线时间戳，也即从1970年1月1日（UTC/GMT的午夜）开始所经过的毫秒
    providerConfig.put(GlobalConstants.CommonKey.TIMESTAMP, System.currentTimeMillis());

    /**
     * 进程id
     */
    providerConfig.put(GlobalConstants.CommonKey.PID, ProcessUtils.getProcessId());

    BusinessResult result = new BusinessResult(true, null);
    return result;
  }

  /**
   * 保存服务提供者配置信息
   *
   * @author sxp
   * @since 2018/12/1
   */
  private void saveServicesConfig() {
    Map<String, Map<String, Object>> currentServicesConfig = ServiceConfigUtils.getCurrentServicesConfig();

    List<ConfigFile> allConf = ConfigFileHelper.getProvider();
    Map<String, Object> confItem;
    Object value;
    String interfaceName, confKey;
    String[] keysFromParam = {GlobalConstants.Provider.Key.INTERFACE, GlobalConstants.CommonKey.METHODS,
            GlobalConstants.PROVIDER_SERVICE_PORT};
    String[] keysOfAuto = {GlobalConstants.CommonKey.TIMESTAMP, GlobalConstants.CommonKey.PID};

    for (Map<String, Object> map : servicesParams) {
      interfaceName = null;
      confItem = new LinkedHashMap<String, Object>(ProviderConstants.CONFIG_CAPACITY);

      for (String key : keysFromParam) {
        value = (map.containsKey(key)) ? (map.get(key)) : (null);
        confItem.put(key, value);

        if (GlobalConstants.Provider.Key.INTERFACE.equals(key)) {
          interfaceName = (String) value;
        }
      }

      for (String key : keysOfAuto) {
        value = (providerConfig.containsKey(key)) ? (providerConfig.get(key)) : (null);
        confItem.put(key, value);
      }

      for (ConfigFile conf : allConf) {
        confKey = conf.getName();
        value = (providerConfig.containsKey(confKey)) ? (providerConfig.get(confKey)) : (null);
        confItem.put(confKey, value);
      }

      servicesConfig.add(confItem);

      /**
       * 向【所有服务提供者的配置信息】也提交一份
       */
      currentServicesConfig.put(interfaceName, confItem);
    }
  }

  /**
   * 注册业务逻辑
   *
   * @author sxp
   * @since 2018/12/1
   */
  private void doRegister() throws Exception {
    Provider provide;
    ProvidersListener listener;
    URL url;
    Map<String, String> providerInfo;
    Map<String, String> parameters;
    Map<String, Object> info;
    Object value;
    String valueOfS, interfaceName, application;
    int port;

    for (Map<String, Object> confItem : servicesConfig) {
      interfaceName = (String) confItem.get(GlobalConstants.Provider.Key.INTERFACE);
      application = (String) confItem.get(GlobalConstants.Provider.Key.APPLICATION);

      Preconditions.checkNotNull(interfaceName, "interfaceName");

      port = ((Integer) confItem.get(GlobalConstants.PROVIDER_SERVICE_PORT)).intValue();

      providerInfo = new HashMap<String, String>();
      for (Map.Entry<String, Object> entry : confItem.entrySet()) {
        if (GlobalConstants.PROVIDER_SERVICE_PORT.equals(entry.getKey())) {
          continue;
        }

        value = entry.getValue();
        valueOfS = (value == null) ? (null) : String.valueOf(value);
        providerInfo.put(entry.getKey(), valueOfS);
      }

      /**
       * 注册服务(providers)
       */
      providerInfo.put(RegistryConstants.CATEGORY_KEY, RegistryConstants.PROVIDERS_CATEGORY);
      parameters = new HashMap<String, String>(providerInfo);

      url = new URL(RegistryConstants.GRPC_PROTOCOL, ip, port, parameters);
      provide = new Provider();
      provide.registerService(url);

      /**
       * 订阅监听器(configurators)
       */
      providerInfo.put(RegistryConstants.CATEGORY_KEY, RegistryConstants.CONFIGURATORS_CATEGORY);
      parameters = new HashMap<String, String>(providerInfo);

      url = new URL(RegistryConstants.OVERRIDE_PROTOCOL, ip, port, parameters);
      listener = new ProvidersListener(interfaceName, ip, application, port);
      provide.subscribe(url, listener);

      /**
       * 将url和监听器保存起来，服务关闭时需要注销监听器
       */
      info = new HashMap<String, Object>();
      info.put("url", url);
      info.put("listener", listener);
      listenersInfo.add(info);
    }
  }
}
