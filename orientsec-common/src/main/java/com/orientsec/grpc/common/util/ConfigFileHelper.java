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

import com.orientsec.grpc.common.OrientsecGrpcVersion;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.enums.DataType;
import com.orientsec.grpc.common.model.ConfigFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置文件助手
 * <p>
 * 这里的配置文件指的是{@link GlobalConstants#CONFIG_FILE_PATH }
 * <p/>
 *
 * @author sxp
 * @since V1.0 2017/3/23
 */
public final class ConfigFileHelper {
  /**
   * 服务提供者的参数名称的前缀
   */
  public static final String PROVIDER_KEY_PREFIX = "provider.";

  /**
   * 服务消费者的参数名称的前缀
   */
  public static final String CONSUMER_KEY_PREFIX = "consumer.";

  /**
   * 配置文件中公共配参数名称的前缀
   */
  public static final String COMMON_KEY_PREFIX = "common.";

  /**
   * 配置文件中以common开头的属性的key值(key值已经去掉了common.的前缀)
   */
  public static final Map<String, Boolean> confFileCommonKeys = new HashMap<String, Boolean>(6);

  /**
   * 服务提供者的参数
   */
  private static final List<ConfigFile> provider = new ArrayList<ConfigFile>(32);

  /**
   * 服务提供者的参数
   */
  private static final List<ConfigFile> consumer = new ArrayList<ConfigFile>(32);

  static {
    initProvider();
    initConsumer();
    initConfFileCommonKeys();
  }

  /**
   * 配置文件中以common开头的属性的key值
   */
  private static void initConfFileCommonKeys() {
    confFileCommonKeys.put(GlobalConstants.CommonKey.APPLICATION, Boolean.TRUE);
    confFileCommonKeys.put(GlobalConstants.CommonKey.GRPC, Boolean.TRUE);
    confFileCommonKeys.put(GlobalConstants.CommonKey.PROJECT, Boolean.TRUE);
    confFileCommonKeys.put(GlobalConstants.CommonKey.OWNER, Boolean.TRUE);
    confFileCommonKeys.put(GlobalConstants.CommonKey.OPERATION_MANAGER, Boolean.TRUE);
  }

  /**
   * 初始化服务提供者配置文件参数
   */
  private static void initProvider() {
    // 必填项
    ConfigFile application = new ConfigFile("application", DataType.STRING, true, null);
    ConfigFile project = new ConfigFile("project", DataType.STRING, true, null);
    ConfigFile owner = new ConfigFile("owner", DataType.STRING, true, null);
    ConfigFile version = new ConfigFile("version", DataType.STRING, true, null);
    ConfigFile grpc = new ConfigFile("grpc", DataType.STRING, true, OrientsecGrpcVersion.VERSION);


    // 必填项-->固定参数
    ConfigFile side = new ConfigFile("side", DataType.STRING, false, "provider");

    // 必填项-系统自动获取
    // ConfigFile interfaceObj = new ConfigFile("interface", DataType.STRING, true, ConfigFile.AUTO_VALUE);
    // ConfigFile methods = new ConfigFile("methods", DataType.STRING, true, ConfigFile.AUTO_VALUE);
    // ConfigFile timestamp = new ConfigFile("timestamp", DataType.LONG, true, ConfigFile.AUTO_VALUE);
    // ConfigFile pid = new ConfigFile("pid", DataType.LONG, true, ConfigFile.AUTO_VALUE);

    // 可选
    ConfigFile ops = new ConfigFile("ops", DataType.STRING, false, null);
    ConfigFile module = new ConfigFile("module", DataType.STRING, false, null);
    ConfigFile group = new ConfigFile("group", DataType.STRING, false, null);
    ConfigFile timeout = new ConfigFile("default.timeout", DataType.INTEGER, false, "1000");
    ConfigFile reties = new ConfigFile("default.reties", DataType.INTEGER, false, "2");
    ConfigFile requests = new ConfigFile("default.requests", DataType.INTEGER, false, String.valueOf(GlobalConstants.Provider.DEFAULT_REQUESTS_NUM));
    ConfigFile connections = new ConfigFile("default.connections", DataType.INTEGER, false, String.valueOf(GlobalConstants.Provider.DEFAULT_CONNECTIONS_NUM));
    ConfigFile loadbalance = new ConfigFile("default.loadbalance", DataType.STRING, false, GlobalConstants.LB_STRATEGY.PICK_FIRST.getSimpleName());
    ConfigFile async = new ConfigFile("default.async", DataType.BOOLEAN, false, "false");
    ConfigFile token = new ConfigFile("token", DataType.STRING, false, "false");
    ConfigFile deprecated = new ConfigFile("deprecated", DataType.BOOLEAN, false, "false");
    ConfigFile dynamic = new ConfigFile("dynamic", DataType.BOOLEAN, false, "true");
    ConfigFile accesslog = new ConfigFile("accesslog", DataType.STRING, false, "false");
    ConfigFile weight = new ConfigFile("weight", DataType.INTEGER, false, "100");
    ConfigFile cluster = new ConfigFile("default.cluster", DataType.STRING, false, "failover");
    ConfigFile appVersion = new ConfigFile("application.version", DataType.STRING, false, null);
    ConfigFile organization = new ConfigFile("organization", DataType.STRING, false, null);
    ConfigFile environment = new ConfigFile("environment", DataType.STRING, false, null);
    ConfigFile moduleVersion = new ConfigFile("module.version", DataType.STRING, false, null);
    ConfigFile anyhost = new ConfigFile("anyhost", DataType.BOOLEAN, false, "false");
    ConfigFile dubbo = new ConfigFile("dubbo", DataType.STRING, false, null);

    ConfigFile accessProtected = new ConfigFile("access.protected", DataType.BOOLEAN, false, "false");
    ConfigFile master = new ConfigFile("master", DataType.BOOLEAN, false, "true");

    provider.add(application);
    provider.add(project);
    provider.add(version);
    provider.add(grpc);
    provider.add(side);

    // provider.add(interfaceObj);
    // provider.add(methods);
    // provider.add(timestamp);
    // provider.add(pid);

    provider.add(ops);
    provider.add(module);
    provider.add(group);
    provider.add(timeout);
    provider.add(reties);
    provider.add(requests);
    provider.add(connections);
    provider.add(loadbalance);
    provider.add(async);
    provider.add(token);
    provider.add(deprecated);
    provider.add(dynamic);
    provider.add(accesslog);
    provider.add(owner);
    provider.add(weight);
    provider.add(cluster);
    provider.add(appVersion);
    provider.add(organization);
    provider.add(environment);
    provider.add(moduleVersion);
    provider.add(anyhost);
    provider.add(dubbo);
    provider.add(accessProtected);
    provider.add(master);
  }

  public static List<ConfigFile> getProvider() {
    return provider;
  }



  /**
   * 初始化服务消费者配置文件参数
   */
  private static void initConsumer() {
    // 必填项
    ConfigFile application = new ConfigFile("application", DataType.STRING, true, null);
    ConfigFile project = new ConfigFile("project", DataType.STRING, true, null);
    ConfigFile owner = new ConfigFile("owner", DataType.STRING, true, null);
    ConfigFile grpc = new ConfigFile("grpc", DataType.STRING, true, OrientsecGrpcVersion.VERSION);

    // 必填项-->固定参数
    ConfigFile side = new ConfigFile("side", DataType.STRING, false, "consumer");
    ConfigFile category = new ConfigFile("category", DataType.STRING, false, "consumers");

    // 必填项-->兼容dubbo的冗余参数
    ConfigFile check = new ConfigFile("check", DataType.BOOLEAN, false, "true");
    ConfigFile timeout = new ConfigFile("default.timeout", DataType.LONG, false, "1000");

    // 必填项-系统自动获取
    // ConfigFile interfaceObj = new ConfigFile("interface", DataType.STRING, true, ConfigFile.AUTO_VALUE);
    // ConfigFile methods = new ConfigFile("methods", DataType.STRING, true, ConfigFile.AUTO_VALUE);
    // ConfigFile timestamp = new ConfigFile("timestamp", DataType.LONG, true, ConfigFile.AUTO_VALUE);

    // 可选
    ConfigFile ops = new ConfigFile("ops", DataType.STRING, false, null);
    ConfigFile serviceVersion = new ConfigFile("service.version", DataType.STRING, false, null);
    ConfigFile applicationVersion = new ConfigFile("application.version", DataType.STRING, false, null);
    ConfigFile filter = new ConfigFile("default.reference.filter", DataType.STRING, false, null);
    ConfigFile logger = new ConfigFile("logger", DataType.STRING, false, null);
    ConfigFile organization = new ConfigFile("organization", DataType.STRING, false, null);
    ConfigFile pid = new ConfigFile("pid", DataType.LONG, false, null);
    ConfigFile retries = new ConfigFile("default.retries", DataType.LONG, false, "2");
    ConfigFile loadbalance = new ConfigFile("default.loadbalance", DataType.STRING, false, GlobalConstants.LB_STRATEGY.PICK_FIRST.getSimpleName());
    ConfigFile requests = new ConfigFile("default.requests", DataType.STRING, false, "0");
    ConfigFile connections = new ConfigFile("default.connections", DataType.STRING, false, "0");
    ConfigFile cluster = new ConfigFile("default.cluster", DataType.STRING, false, "failover");
    ConfigFile invokeGroup = new ConfigFile("invoke.group", DataType.STRING, false, null);


    consumer.add(application);
    consumer.add(project);

    consumer.add(category);
    consumer.add(check);
    consumer.add(grpc);
    consumer.add(side);
    consumer.add(timeout);
    consumer.add(ops);
    consumer.add(serviceVersion);
    consumer.add(applicationVersion);
    consumer.add(filter);
    consumer.add(logger);
    consumer.add(owner);
    consumer.add(organization);
    consumer.add(pid);
    consumer.add(retries);
    consumer.add(loadbalance);
    consumer.add(requests);
    consumer.add(connections);
    consumer.add(cluster);
    consumer.add(invokeGroup);
  }

  public static List<ConfigFile> getConsumer() {
    return consumer;
  }
}
