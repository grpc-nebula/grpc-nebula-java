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

import java.util.concurrent.atomic.AtomicInteger;


/**
 * Greeter服务实现类
 */
public class GreeterImpl extends GreeterGrpc.GreeterImplBase {
  private boolean isInvoked = false;
  private AtomicInteger invokedTimes = new AtomicInteger(0);
  private boolean breakdown = false;

  /**
   * sayHello方法实现
   */
  public void sayHello(GreeterRequest req, StreamObserver<GreeterReply> responseObserver) {
    GreeterReply reply = GreeterReply.newBuilder().setMessage(("Hello World")).build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }


  public void doSomethingForLongTime(CommonRequest req, StreamObserver<CommonReply> responseObserver) {
    CommonReply reply = CommonReply.newBuilder().setMessage(("Hello World: " + req.getName()) ).build();
    try {
      Thread.sleep(1000);// 1秒钟
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }

  /**
   * 测试该方法有没有被调用过
   *
   * @author sxp
   * @since 2018-8-8
   */
  public void checkMethodInvoked(CommonRequest request, StreamObserver<CommonReply> responseObserver) {
    if (breakdown) {
      throw new RuntimeException("模拟服务端的异常.");
    }

    isInvoked = true;
    invokedTimes.incrementAndGet();

    CommonReply reply = CommonReply.newBuilder().setSuccess(true).build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }

  /**
   * 获取checkMethodInvoked是否被调用过
   *
   * @author sxp
   * @since 2018-8-8
   */
  public boolean isIsInvoked() {
    return isInvoked;
  }

  public void setInvoked(boolean invoked) {
    isInvoked = invoked;
  }

  public int getInvokedTimes() {
    return invokedTimes.get();
  }

  public void setInvokedTimes(int newInvokedTimes) {
    invokedTimes.set(newInvokedTimes);
  }

  public boolean isBreakdown() {
    return breakdown;
  }

  public void setBreakdown(boolean breakdown) {
    this.breakdown = breakdown;
  }
}
