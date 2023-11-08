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
import com.orientsec.grpc.common.constant.RegistryConstants;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.service.Consumer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestConsumer {
  private Consumer consumer;

  public TestConsumer(){
    Init();
  }

  public void Init(){
    URL zkUrl = new URL("zookeeper", TestUtils.ZOOKEEPER_IP, TestUtils.ZOOKEEPER_PORT);
    consumer = new Consumer(zkUrl);
  }

  public void testRegistryAndUnRegistry(){
    Map<String, String> parameters = TestUtils.createParameters(null);
    parameters.put("category","consumers");
    parameters.put("side","consumer");
    URL consumerUrl = new URL("consumer", "127.127.127.0", 9999,parameters);
    System.out.println("Test Consumer Registry start ...");
    consumer.registerService(consumerUrl);
    List<URL> urls = consumer.lookup(consumerUrl);
    boolean bFlag = false;
    for (URL url : urls){
      if (consumerUrl.toString().equals(url.toString())){
        System.out.println("Test Consumer Registry SUCCESS ...");
        bFlag = true;
        break;
      }
    }
    if (!bFlag){
      System.out.println("Test Consumer Registry FAILURE ...");
      return;
    }
    System.out.println("Test Consumer UnRegistry start ...");
    consumer.unRegisterService(consumerUrl);
    List<URL> urls2 = consumer.lookup(consumerUrl);
    bFlag = false;
    for (URL url : urls2){
      if (consumerUrl.toString().equals(url.toString())){
        System.out.println("Test Consumer UnRegistry FAILURE ...");
        bFlag = true;
        break;
      }
    }
    if (!bFlag){
      System.out.println("Test Consumer UnRegistry SUCCESS ...");
    }
    System.out.println("Test Consumer finished ...");
  }


  public void testLookup(){
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(RegistryConstants.CATEGORY_KEY, RegistryConstants.PROVIDERS_CATEGORY);
    parameters.put(GlobalConstants.CommonKey.INTERFACE, "com.orientsec.grpc.examples.helloworld.Greeter");
    parameters.put(GlobalConstants.CommonKey.VERSION, "1.0.0");

    URL serviceUrl = new URL(RegistryConstants.GRPC_PROTOCOL, RegistryConstants.ANYHOST_VALUE, 0, parameters);
    List<URL> services = consumer.lookup(serviceUrl);
    for (URL url: services){
      System.out.println("service url=" + url.toString());
    }
  }

  public static void main(String[] args) {
    TestConsumer testConsumer = new TestConsumer();
    testConsumer.testLookup();
    System.out.println("wait");
  }
}
