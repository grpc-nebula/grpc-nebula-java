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
package com.orientsec.grpc.common;

import com.orientsec.grpc.common.resource.SystemConfig;

import java.util.Properties;

/**
 * 常量
 *
 * @author sxp
 * @since 2018/7/20
 */
public class Constants {
  private static final Properties properties = SystemConfig.getProperties();
  
  public static class Port {
    public static final int SINGLE_SERVICE_SERVER = 51001;
    public static final int TWO_SERVICES_SERVER = 51002;
    public static final int COMMON_SERVICE_SERVER = 51003;
    public static final int COMMON_SERVICE_SECOND_SERVER = 51004;
    public static final int ROUTE_GUIDE_SERVER = 51005;
    public static final int PERSON_SERVICE_SERVER = 51006;

    public static final int KafkaSender_SERVER_1 = 50080;
    public static final int KafkaSender_SERVER_2 = 50090;

    public static final int ERROR_TEST_SERVER = 51099;
  }

  public static class Kafka {
    public static String SERVERS = properties.getProperty("kafka.servers");
    public static final String TOPIC = properties.getProperty("kafka.topic");
    public static final String COMSUMER_GROUP_ID = "grpc-java-functional-test";
  }


  /**
   * 系统属性
   */
  public static final Properties SYSTEM_PROPERTIES = System.getProperties();

  /**
   * 操作系统名称
   */
  public static final String OS_NAME = SYSTEM_PROPERTIES.getProperty("os.name");

  /**
   * 文件分隔符
   * <p>
   * Linux下为/ <br>
   * Windows下为\ <br>
   * </p>
   */
  public static final String FILE_SEPARATOR = SYSTEM_PROPERTIES.getProperty("file.separator");

  /**
   * 当前操作系统是否为Windows
   */
  public static boolean IS_WINDOWS_OS = false;


  static {
    if (OS_NAME.indexOf("Windows") >= 0 && FILE_SEPARATOR.equals("\\")) {
      IS_WINDOWS_OS = true;
    } else {
      IS_WINDOWS_OS = false;
    }
  }

}
