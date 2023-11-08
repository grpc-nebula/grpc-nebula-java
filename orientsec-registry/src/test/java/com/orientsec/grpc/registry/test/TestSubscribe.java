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

import com.orientsec.grpc.registry.NotifyListener;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.service.Consumer;


import java.util.List;
import java.util.Map;

class DemoListener implements NotifyListener{

  @Override
  public void notify(List<URL> urls) {
      for (URL url : urls) {
        System.out.println("providers changed:" + url.toString());
      }
  }
}

public class TestSubscribe {
  public static void main(String[] args) {
    URL zkUrl = new URL("zookeeper",TestUtils.ZOOKEEPER_IP, TestUtils.ZOOKEEPER_PORT);
    Consumer consumer = new Consumer(zkUrl);
    Map<String,String> parameters = TestUtils.createParameters(null);
    URL serviceUrl = new URL("grpc","127.0.0.1",8080,parameters);
    DemoListener listener = new DemoListener();
    consumer.subscribe(serviceUrl,listener);
    System.out.println("wait");
    try{
      Thread.currentThread().sleep(50000);
    }catch(InterruptedException ie){
      ie.printStackTrace();
    }
  }
}
