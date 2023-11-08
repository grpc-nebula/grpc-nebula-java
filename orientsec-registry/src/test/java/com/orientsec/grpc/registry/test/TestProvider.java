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

import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.exception.PropertiesException;
import com.orientsec.grpc.registry.service.Provider;

import java.util.List;
import java.util.Map;

/**
 * provider测试类.
 */
public class TestProvider {

  private Provider provider;

  public TestProvider(){
    init();
  }

  public void init(){
    URL zkUrl = new URL("zookeeper",TestUtils.ZOOKEEPER_IP, TestUtils.ZOOKEEPER_PORT);
    //provider = new Provider(zkUrl);
    try {
      provider = new Provider();
    } catch (PropertiesException e) {
      e.printStackTrace();
    }
  }

  public void testRegistry(String ip,int port){
    Map<String, String> parameters = TestUtils.createParameters(null);
    URL providerUrl = new URL("grpc", ip, port,parameters);
    provider.registerService(providerUrl);
    List<URL> urls = provider.lookup(providerUrl);
    for (URL url : urls){
      if (providerUrl.toString().equals(url.toString())){
        System.out.println("Registry service success");
        break;
      }
    }
    System.out.println("Registry service failed");
  }

  public void testUnRegistry(String ip,int port){
    Map<String, String> parameters = TestUtils.createParameters(null);
    URL providerUrl = new URL("grpc", ip, port,parameters);
    provider.unRegisterService(providerUrl);
  }


  public void testRegistryAndUnRegistry(){
    Map<String, String> parameters = TestUtils.createParameters(null);
    URL providerUrl = new URL("grpc", "127.127.127.0", 9999,parameters);
    System.out.println("Test Provider Registry start ...");
    provider.registerService(providerUrl);
    List<URL> urls = provider.lookup(providerUrl);
    boolean bFlag = false;
    for (URL url : urls){
      if (providerUrl.toString().equals(url.toString())){
        System.out.println("Test Provider Registry SUCCESS ...");
        bFlag = true;
        break;
      }
    }
    if (!bFlag){
      System.out.println("Test Provider Registry FAILURE ...");
      return;
    }
    System.out.println("Test Provider UnRegistry start ...");
    provider.unRegisterService(providerUrl);
    List<URL> urls2 = provider.lookup(providerUrl);
    bFlag = false;
    for (URL url : urls2){
      if (providerUrl.toString().equals(url.toString())){
        System.out.println("Test Provider UnRegistry FAILURE ...");
        bFlag = true;
        break;
      }
    }
    if (!bFlag){
      System.out.println("Test Provider UnRegistry SUCCESS ...");
    }
    System.out.println("Test Provider finished ...");
  }

  /**
   * 主函数.
   * @param args 命令行参数
   */
  public static void main(String[] args) {

    TestProvider testProvider = new TestProvider();
    testProvider.testRegistryAndUnRegistry();
    System.out.println("wait");
  }


}
