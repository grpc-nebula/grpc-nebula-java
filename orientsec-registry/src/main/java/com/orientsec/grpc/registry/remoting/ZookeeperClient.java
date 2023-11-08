/*
 * Copyright 2018-2019 The Apache Software Foundation
 * Modifications 2019 Orient Securities Co., Ltd.
 * Modifications 2019 BoCloud Inc.
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


package com.orientsec.grpc.registry.remoting;

import com.orientsec.grpc.registry.common.URL;

import java.util.List;

/**
 * Created by heiden on 2017/3/15.
 */
public interface ZookeeperClient {

  void create(String path, boolean ephemeral);

  void delete(String path);

  String getData(String path);

  List<String> getChildren(String path);

  List<String> addChildListener(String path, ChildListener listener);

  void removeChildListener(String path, ChildListener listener);

  void addStateListener(StateListener listener);

  void removeStateListener(StateListener listener);

  boolean isConnected();

  void close();

  URL getUrl();

}
