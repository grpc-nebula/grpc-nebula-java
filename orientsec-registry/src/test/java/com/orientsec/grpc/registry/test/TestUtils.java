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
package com.orientsec.grpc.registry.test;

import com.orientsec.grpc.common.constant.GlobalConstants;

import java.util.HashMap;
import java.util.Map;

public class TestUtils {

  public static final String ZOOKEEPER_IP = "127.0.0.1";
  public static final int ZOOKEEPER_PORT = 2181;

  public static final String inteface = "com.orientsec.sproc2grpc.service.SprocService";

  public static Map<String, String> createParameters(Map<String, String> parameters){
    if (parameters == null){
      parameters = new HashMap<String, String>();
    }
    parameters.put("application","com.orientsec.grpc.test1");
    parameters.put("version","1.0.0");
    parameters.put("module","grpc-registry-test");
    parameters.put(GlobalConstants.CommonKey.METHODS,"get,set,test");
    parameters.put("side","provider");
    parameters.put(GlobalConstants.CommonKey.TIMESTAMP, String.valueOf(System.currentTimeMillis()));
    parameters.put("grpc","1.1.2-released");
    parameters.put("anyhost","true");
    parameters.put("interface",inteface);
    parameters.put("dynamic","false");
    return parameters;
  }
}
