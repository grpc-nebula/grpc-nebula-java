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

import com.orientsec.grpc.common.exception.BusinessException;
import com.orientsec.grpc.common.resource.SystemSwitch;
import com.orientsec.grpc.common.util.IpUtils;
import com.orientsec.grpc.consumer.common.ConsumerConstants;
import com.orientsec.grpc.consumer.task.LookupTask;
import com.orientsec.grpc.consumer.task.RegistryTask;
import com.orientsec.grpc.consumer.task.UnRegistryTask;
import com.orientsec.grpc.consumer.watch.ConsumerListener;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.exception.PropertiesException;
import com.orientsec.grpc.registry.service.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;




/**
 * 默认的服务注册
 * <p>
 * 1.消费者请求服务时，自动注册客户端 <br>
 * 注册时同时订阅相关目录
 * </p>
 *
 * @author dengjianquan
 * @since 2018/3/30
 */
public class DefaultConsumerServiceRegistryImpl implements ConsumerServiceRegistry {
  private static final Logger logger = LoggerFactory.getLogger(DefaultConsumerServiceRegistryImpl.class);

  private Consumer consumer;
  private boolean consumerInit = false;
  /**
   * consumer的的配置
   */
  private Map<String, Object> consumerParmas;
  /**
   * 当前客户端上的注册url和监听器
   */
  private Map<String, Object> listenersInfo = new ConcurrentHashMap<String, Object>();
  /**
   * consumer的IP（本地的IP）
   */
  private String ip;
  /**
   * 服务的版本
   */
  private String version;
  private URL targetUrl;

  public DefaultConsumerServiceRegistryImpl() {
  }

  public DefaultConsumerServiceRegistryImpl(URL targetUrl) {
    this.targetUrl = targetUrl;
  }

  public boolean isConsumerInit() {
    return consumerInit;
  }

  public Consumer getConsumer() {
    return consumer;

  }

  public ConsumerServiceRegistry forTarget(URL targetUrl) {
    this.targetUrl = targetUrl;
    return this;
  }


  public ConsumerServiceRegistry build() {
    initConsumerService();
    return this;
  }

  /**
   * 初始化服务消费者
   *
   * @author sxp
   * @since 2018/12/1
   */
  private void initConsumerService() {
    try {
      if (targetUrl != null) {
        consumer = new Consumer(targetUrl);
      } else {
        consumer = new Consumer();
      }
      consumerInit = true;
    } catch (PropertiesException e) {
      logger.error(e.getMessage(), e);
    }

  }

  /**
   * 注册
   *
   * @param consumerParams        消费者参数map
   * @param providersListener     providers订阅回调函数
   * @param routersListener       routers订阅回调函数
   * @param configuratorsListener configurators订阅回调函数
   * @return String
   * @throws BusinessException
   */
  public String register(Map<String, Object> consumerParams,
                         ConsumerListener providersListener,
                         ConsumerListener routersListener,
                         ConsumerListener configuratorsListener) throws BusinessException {

    if (!SystemSwitch.CONSUMER_ENABLED) {
      return "";
    }

    this.consumerParmas = consumerParams;

    if (providersListener != null) {
      listenersInfo.put(ConsumerConstants.PROVIDERS_LISTENER_KEY, providersListener);
    }
    if (routersListener != null) {
      listenersInfo.put(ConsumerConstants.ROUTERS_LISTENER_KEY, routersListener);
    }
    if (configuratorsListener != null) {
      listenersInfo.put(ConsumerConstants.CONFIGURATORS_LISTENER_KEY, configuratorsListener);
    }

    ip = IpUtils.getIP4WithPriority();

    RegistryTask registerTask = new RegistryTask(this, consumerParams, listenersInfo);
    String uuid;
    try {
      uuid = registerTask.work();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new BusinessException(e.getMessage(), e);
    }
    //缓存本次订阅信息
    Map<String, Map<String, Object>> allConsumersListeners = ConsumerConfigUtils.getAllConsumersListeners();
    Map<String, Map<String, Object>> allConsumersConfig = ConsumerConfigUtils.getAllConsumersConfig();
    allConsumersConfig.put(uuid, consumerParams);
    allConsumersListeners.put(uuid, listenersInfo);

    Map<String, Map<String, Object>> allConsumersInitConfig = ConsumerConfigUtils.getAllConsumersInitConfig();
    allConsumersInitConfig.put(uuid, new HashMap<>(consumerParams));

    return uuid;
  }

  /**
   * 取消订阅
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void unSubscribe(String subscribeId) throws BusinessException {
    if (!SystemSwitch.CONSUMER_ENABLED) {
      return;
    }

    Map<String, Map<String, Object>> allConsumersListeners = ConsumerConfigUtils.getAllConsumersListeners();
    Map<String, Map<String, Object>> allConsumersConfig = ConsumerConfigUtils.getAllConsumersConfig();

    boolean isRegular;
    UnRegistryTask unRegistryTask = null;
    if (subscribeId == null) {
      if (consumerParmas == null || listenersInfo.size() == 0) {
        logger.warn("接口unSubscribe非法调用，unSubscribe必须在subscribe之后调用");
        return;
      }
      unRegistryTask = new UnRegistryTask(this, this.consumerParmas, listenersInfo);
      isRegular = true;
    } else {
      if (!allConsumersListeners.containsKey(subscribeId)) {
        logger.info("缓存中未能找到对应[" + subscribeId + "]的订阅");
        return;
      }
      unRegistryTask = new UnRegistryTask(this,
              allConsumersConfig.get(subscribeId),
              allConsumersListeners.get(subscribeId));
      isRegular = false;
    }


    try {
      unRegistryTask.work();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new BusinessException(e.getMessage(), e);
    }
    //清除缓存内容
    if (isRegular) {
      ConsumerConfigUtils.releaseConsumerConfig(this.consumerParmas);
    } else {
      ConsumerConfigUtils.releaseConsumerConfig(subscribeId);
    }

  }

  /**
   * 查找符合条件的URL信息
   *
   * @author sxp
   * @since 2018/12/1
   */
  public List<URL> lookup(Map<String, String> providerParams) throws BusinessException {
    LookupTask lookupTask = new LookupTask(this, providerParams);
    List<URL> providersUrls;
    try {
      providersUrls = lookupTask.work();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new BusinessException(e.getMessage(), e);
    }

    return providersUrls;
  }

  // ---- getters and setters ----

  public Map<String, Object> getConsumerConfig() {
    return consumerParmas;
  }

  public Map<String, Object> getListenersInfo() {
    return listenersInfo;
  }

  /**
   * 获取IP
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getIp() {
    return ip;
  }

  /**
   * 获取版本
   *
   * @author sxp
   * @since 2018/12/1
   */
  public String getVersion() {
    return version;
  }

  /**
   * 设置版本
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setVersion(String version) {
    this.version = version;
  }
}
