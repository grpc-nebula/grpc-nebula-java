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


import com.orientsec.grpc.service.QueryGrpc;
import com.orientsec.grpc.service.QueryReply;
import com.orientsec.grpc.service.QueryRequest;
import io.grpc.stub.StreamObserver;


/**
 * query服务实现类
 */
public class QueryImpl extends QueryGrpc.QueryImplBase {
  /**
   * query方法实现.
   */
  public void query(QueryRequest request, StreamObserver<QueryReply> responseObserver) {
    QueryReply reply = QueryReply.newBuilder().setMessage("com.orientsec.grpc.examples.impl.QueryImpl.query")
            .setNo(123456)
            .setName("施小平")
            .setAlias("别名")
            .setSex(true)
            .setSalary(10000.00)
            .setTotal(10000000)
            .setDesc("这是一个查询的例子")
            .setMisc("101 Romantic Ideas")
            .build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }


}
