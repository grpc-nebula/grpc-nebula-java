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


package com.orientsec.grpc.service.impl;


import com.orientsec.grpc.service.CommonReply;
import com.orientsec.grpc.service.CommonRequest;
import com.orientsec.grpc.service.GreeterGrpc;
import com.orientsec.grpc.service.GreeterReply;
import com.orientsec.grpc.service.GreeterRequest;
import io.grpc.stub.StreamObserver;


/**
 * Greeter服务实现类
 */
public class GreeterImpl2 extends GreeterGrpc.GreeterImplBase {
	private int tag;
	public GreeterImpl2(int port){
		tag = port;
	}
  /**
   * sayHello方法实现
   */
  public void sayHello(GreeterRequest req, StreamObserver<GreeterReply> responseObserver) {
    GreeterReply reply = GreeterReply.newBuilder().setMessage(("Hello World：" + tag)).build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }

  /**
   *  测试5秒内的并发数。
   */
  public void doSomethingForLongTime(CommonRequest req, StreamObserver<CommonReply> responseObserver) {
    System.out.println("doSomethingForLongTime is Called begin.....");

    CommonReply reply = CommonReply.newBuilder().setMessage(("Hello World: " + req.getName()) ).build();
    try {
      Thread.sleep(1000);// 1秒钟
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    responseObserver.onNext(reply);
    responseObserver.onCompleted();

    System.out.println("doSomethingForLongTime is Called end.....");
  }

}
