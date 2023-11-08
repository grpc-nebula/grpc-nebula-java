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
package com.orientsec.grpc.test;

import com.orientsec.grpc.common.TestProjectPropertyUtils;
import com.orientsec.grpc.common.ZkServiceImpl;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.resource.SystemConfig;
import com.orientsec.grpc.consumer.core.ConsumerServiceRegistry;
import com.orientsec.grpc.consumer.core.ConsumerServiceRegistryFactory;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class TestLookup {

  @BeforeClass
  public static void setUp() {
    TestProjectPropertyUtils.setUserDir();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    new ZkServiceImpl().releaseRegistry();
    TestProjectPropertyUtils.recoverUserDir();
  }

  @Test
  public void usage() {
    URL zkUrl = fromConfig();

    ConsumerServiceRegistry registry = ConsumerServiceRegistryFactory.getRegistry();

    registry.forTarget(new URL("zookeeper", zkUrl.getHost(), zkUrl.getPort())).build();

    Map<String, String> params = new ConcurrentHashMap<String, String>();

    params.put(GlobalConstants.Provider.Key.INTERFACE, "com.orientsec.grpc.examples.helloworld.Greeter");
    List<URL> service = registry.lookup(params);

    for (URL url : service) {
      System.out.println("service ip=" + url.getIp() + ",port=" + url.getPort());
    }
  }

  private static URL fromConfig(){
    Properties properties = SystemConfig.getProperties();
    if (properties == null) {
      System.out.println("Cannot load properties:" + GlobalConstants.CONFIG_FILE_PATH);
      return null;
    }
    String addresses = properties.getProperty(GlobalConstants.REGISTRY_CENTTER_ADDRESS);
    if (addresses == null ||
            addresses.length() == 0){
      System.out.println("Cannot find key :" + GlobalConstants.REGISTRY_CENTTER_ADDRESS +
              " in " + GlobalConstants.CONFIG_FILE_PATH);
      return null;
    }
    int index = addresses.indexOf(",");
    String address = "";
    String backupAddr = "";
    Map<String,String> parameters = null;
    if (index < 0){
      address = addresses;
    }else{
      address = addresses.substring(0,index);
      backupAddr = addresses.substring(index + 1);
      parameters = new HashMap<String, String>();
      parameters.put(Constants.BACKUP_KEY,backupAddr);
    }

    String[] ipAndPort = address.split(":");
    String registryIp = null;
    int registryPort = GlobalConstants.Zookeeper.DEFAULT_PORT;
    if (ipAndPort.length == 2){
      registryIp = ipAndPort[0];
      registryPort = Integer.valueOf(ipAndPort[1]);
    }else{
      registryIp = ipAndPort[0];
    }
    URL url = null;
    if (index < 0){
      url = new URL(GlobalConstants.Zookeeper.PROTOCOL_PREFIX,
              registryIp,registryPort);
    }else{
      url = new URL(GlobalConstants.Zookeeper.PROTOCOL_PREFIX,
              registryIp,registryPort,parameters);
    }
    return url;
  }

}
