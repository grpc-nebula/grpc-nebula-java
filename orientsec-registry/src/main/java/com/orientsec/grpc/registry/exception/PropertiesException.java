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
package com.orientsec.grpc.registry.exception;

public class PropertiesException extends Exception {
  private static final long serialVersionUID = -741234435691077114L;

  public PropertiesException() {
  }

  public PropertiesException(String msg) {
    super(msg);
  }

  public PropertiesException(String key, String msg) {
    super("[KEY]" + key + " not found or value is null," + msg);
  }
}
