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

import com.orientsec.grpc.service.PersonAgeGrpc;
import com.orientsec.grpc.service.PersonReply;
import com.orientsec.grpc.service.PersonRequest;
import io.grpc.stub.StreamObserver;

/**
 * 人员年龄服务
 *
 * @author sxp
 * @since 2018/8/18
 */
public class PersonAgeImpl extends PersonAgeGrpc.PersonAgeImplBase {

  /**
   * 查询人员年龄
   *
   * @author sxp
   * @since  2017-8-18
   */
  @Override
  public void queryAge(PersonRequest request,
                        StreamObserver<PersonReply> responseObserver) {
    int no = request.getNo();

    boolean success = true;
    String message = "OK";
    String name = "";
    int age;
    double salary = 0;

    switch (no) {
      case 1:
        age = 18;
        break;
      case 2:
        age = 20;
        break;
      case 3:
        age = 22;
        break;
      default:
        age = 0;
        success = false;
        message = "系统中未查询到no为[" + no + "]的人员信息";
        break;
    }

    PersonReply reply = PersonReply.newBuilder()
            .setSuccess(success)
            .setMessage(message)
            .setNo(no)
            .setName(name)
            .setAge(age)
            .setSalary(salary)
            .build();

    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }
}
