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

import com.orientsec.grpc.service.PersonInfoGrpc;
import com.orientsec.grpc.service.PersonReply;
import com.orientsec.grpc.service.PersonRequest;
import com.orientsec.grpc.service.PersonSalaryGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 人员信息服务
 *
 * @author sxp
 * @since 2018/8/18
 */
public class PersonInfoImpl extends PersonInfoGrpc.PersonInfoImplBase {
  private static Logger logger = Logger.getLogger(PersonInfoImpl.class.getName());
  private static ManagedChannel channel;
  private static PersonSalaryGrpc.PersonSalaryBlockingStub blockingStub;

  static {
    String target = "zookeeper:///" + PersonSalaryGrpc.SERVICE_NAME;

    channel = ManagedChannelBuilder.forTarget(target)
            .usePlaintext()
            .build();

    blockingStub = PersonSalaryGrpc.newBlockingStub(channel);
  }

  /**
   * 查询人员信息
   *
   * @author sxp
   * @since  2017-8-21
   */
  @Override
  public void query(PersonRequest request,
                        StreamObserver<PersonReply> responseObserver) {
    // 当前服务能查询部分人员信息
    PersonReply nameReply = queryName(request);
    PersonReply ageReply = queryAge(request);

    // 薪资信息调用其它服务查询
    boolean success;
    String message = "OK";
    double salary;

    try {
      PersonReply reply = blockingStub.querySalary(request);

      success = reply.getSuccess();

      if (reply.getSuccess()) {
        salary = reply.getSalary();
      } else {
        message = "调用服务取薪资出错，" + reply.getMessage();
        salary = 0;
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);

      success = false;
      message = "调用服务取薪资出错，" + e.getMessage();
      salary = 0;
    }

    // 形成最终的结果
    int no = request.getNo();
    String name = nameReply.getName();
    int age = ageReply.getAge();

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


  private PersonReply queryName(PersonRequest request) {
    int no = request.getNo();

    boolean success = true;
    String message = "OK";
    String name;
    int age = 0;
    double salary = 0;

    switch (no) {
      case 1:
        name = "施小平";
        break;
      case 2:
        name = "丁正刚";
        break;
      case 3:
        name = "吴佳伟";
        break;
      default:
        name = "未知";
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

    return reply;
  }

  private PersonReply queryAge(PersonRequest request) {
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

    return reply;
  }

}
