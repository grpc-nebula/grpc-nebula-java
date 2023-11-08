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

package com.orientsec.grpc.examples.helloworld;

/**
 * TestPay服务实现类
 *
 * @author sxp
 * @since 2019/6/18
 */
public class TestPayImpl extends TestPayGrpc.TestPayImplBase {

  /**
   * consumerPay方法实现.
   * @param request 请求
   * @param responseObserver 响应
   */
  public void consumerPay(PayRequest request,
                          io.grpc.stub.StreamObserver<PayReply> responseObserver) {
    System.out.println("service:" + request.getName());
    PayReply reply = PayReply.newBuilder().setMessage(("pay: " + request.getName())).build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }
}
