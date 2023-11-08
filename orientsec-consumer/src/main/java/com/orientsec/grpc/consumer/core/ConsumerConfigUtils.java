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
package com.orientsec.grpc.consumer.core;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.exception.BusinessException;
import com.orientsec.grpc.common.model.BusinessResult;
import com.orientsec.grpc.common.model.ConfigFile;
import com.orientsec.grpc.common.resource.SystemConfig;
import com.orientsec.grpc.common.util.ConfigFileHelper;
import com.orientsec.grpc.common.util.ProcessUtils;
import com.orientsec.grpc.common.util.StringUtils;
import com.orientsec.grpc.consumer.common.ConsumerConstants;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端配置信息工具类
 *
 * @author djq
 * @since V1.0 2017/3/30
 */
public class ConsumerConfigUtils {
  /**
   * 所有消费者的配置信息
   * <p>
   * key值为客户端的唯一标识,value为存储的是对应消费者的属性键值对
   * </p>
   */
  private static Map<String, Map<String, Object>> allConsumersConfig = new ConcurrentHashMap<>();

  /**
   * 客户端的初始配置
   * <p>
   * key值为客户端的唯一标识,value为存储的是对应消费者的属性键值对
   * </p>
   */
  private static Map<String, Map<String, Object>> allConsumersInitConfig = new ConcurrentHashMap<>();

  /**
   * 所有消费者的配置信息
   * <p>
   * key值为客户端的唯一标识,value为存储的是对应消费者的监听器
   * </p>
   */
  private static Map<String, Map<String, Object>> allConsumersListeners = new ConcurrentHashMap<>();

  private Map<String, Object> consumerConfig = new HashMap<>();

  private Map<String, Object> confItem = new HashMap<>();

  /**
   * 获取所有服务消费者的配置信息
   *
   * @author dengjq
   * @since V1.0 2017/3/22
   */
  public static Map<String, Map<String, Object>> getAllConsumersListeners() {
    return allConsumersListeners;
  }


  /**
   * 获取所有服务消费者的配置信息
   *
   * @author dengjq
   * @since V1.0 2017/3/22
   */
  public static Map<String, Map<String, Object>> getAllConsumersConfig() {
    return allConsumersConfig;
  }

  /**
   * 获取客户端的初始配置
   *
   * @author sxp
   * @since 2019/1/31
   */
  public static Map<String, Map<String, Object>> getAllConsumersInitConfig() {
    return allConsumersInitConfig;
  }

  /**
   * 获取配置条目
   *
   * @author sxp
   * @since 2018/12/1
   */
  public Map<String, Object> getConfItem() {
    return confItem;
  }

  /**
   * 校验和保存配置文件信息
   *
   * @author sxp
   * @since V1.0 2017-3-23
   */
  private BusinessResult checkAndSaveProperties() {
    String msg;

    // 读取服务配置文件
    Properties pros = SystemConfig.getProperties();
    if (pros == null) {
      throw new BusinessException("注册消费端失败:读取配置文件失败，请确认属性文件的路径["
              + GlobalConstants.CONFIG_FILE_PATH + "]是否正确！");
    }
    if (pros == null || pros.isEmpty()) {
      msg = "配置文件[" + GlobalConstants.CONFIG_FILE_PATH + "]中未配置有效属性！";
      return new BusinessResult(false, msg);
    }

    List<ConfigFile> allConf = ConfigFileHelper.getConsumer();
    String proName, value;

    for (ConfigFile conf : allConf) {
      if (ConfigFileHelper.confFileCommonKeys.containsKey(conf.getName())) {
        proName = ConfigFileHelper.COMMON_KEY_PREFIX + conf.getName();// 配置文件中的属性名
      } else {
        proName = ConfigFileHelper.CONSUMER_KEY_PREFIX + conf.getName();
      }

      if (pros.containsKey(proName)) {
        value = pros.getProperty(proName);
        if (value != null) {
          value = value.trim();// 去空格
        }

        consumerConfig.put(conf.getName(), value);
      } else {
        value = null;

        consumerConfig.put(conf.getName(), conf.getDefaultValue());
      }

      // 校验必填项
      if (conf.isRequired() && StringUtils.isEmpty(value)) {
        msg = "配置文件[" + GlobalConstants.CONFIG_FILE_PATH + "]中[" + proName + "]是必填项，属性值不能为空！";
        return new BusinessResult(false, msg);
      }
    }

    // 消费者启动时间戳，也即从1970年1月1日（UTC/GMT的午夜）开始所经过的毫秒
    consumerConfig.put(GlobalConstants.CommonKey.TIMESTAMP, System.currentTimeMillis());
    // 进程id
    consumerConfig.put(GlobalConstants.CommonKey.PID, ProcessUtils.getProcessId());

    BusinessResult result = new BusinessResult(true, null);
    return result;
  }

  /**
   * 合并当前客户端的的配置属性信息
   *
   * @author djq
   * @since V1.0 2017-3-24
   */
  private void saveConsumersConfig(Map<String, Object> consumersParams) {
    List<ConfigFile> allConf = ConfigFileHelper.getConsumer();
    Object value;
    String confKey;
    String[] keysFromParam = {GlobalConstants.Consumer.Key.INTERFACE,
            GlobalConstants.Consumer.Key.CONSUMER_GROUP_KEY};
    String[] keysOfAuto = {GlobalConstants.CommonKey.TIMESTAMP, GlobalConstants.CommonKey.PID};

    confItem = new LinkedHashMap<>(ConsumerConstants.CONFIG_CAPACITY);

    for (String key : keysOfAuto) {
      value = (consumerConfig.containsKey(key)) ? (consumerConfig.get(key)) : (null);
      confItem.put(key, value);
    }

    for (ConfigFile conf : allConf) {
      confKey = conf.getName();
      value = (consumerConfig.containsKey(confKey)) ? (consumerConfig.get(confKey)) : (null);
      confItem.put(confKey, value);
    }

    for (String key : keysFromParam) {
      value = (consumersParams.containsKey(key)) ? (consumersParams.get(key)) : (null);
      confItem.put(key, value);
    }
  }

  /**
   * 合并服务消费者配置信息
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static Map<String, Object> mergeConsumerConfig(Map<String, Object> consumersParams) {
    ConsumerConfigUtils consumerConfigUtils = new ConsumerConfigUtils();

    BusinessResult result = consumerConfigUtils.checkAndSaveProperties();
    if (!result.isSuccess()) {
      throw new BusinessException("客户端注册失败:" + result.getMessage());
    }

    consumerConfigUtils.saveConsumersConfig(consumersParams);
    return consumerConfigUtils.getConfItem();
  }

  /**
   * 释放服务消费者配置信息
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static void releaseConsumerConfig(Map<String, Object> consumersParams) {
    String subscribeId = "";
    for (Map.Entry<String,Map<String,Object>> consumerConfig: allConsumersConfig.entrySet()){
      if (consumerConfig.getValue() == consumersParams){
        subscribeId = consumerConfig.getKey();
        break;
      }
    }
    if (!subscribeId.isEmpty()){
      releaseConsumerConfig(subscribeId);
    }
    return;
  }

  /**
   * 释放服务消费者配置信息(不同入参)
   *
   * @author sxp
   * @since 2018/12/1
   */
  public static void releaseConsumerConfig(String subscribeId) {
    allConsumersConfig.remove(subscribeId);
    allConsumersInitConfig.remove(subscribeId);

    if (allConsumersListeners.containsKey(subscribeId)) {
      Map<String, Object> listener = allConsumersListeners.get(subscribeId);
      if (listener != null) {
        listener.clear();
      }
      allConsumersListeners.remove(subscribeId);
    }
  }

}
