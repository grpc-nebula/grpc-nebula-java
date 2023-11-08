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
package com.orientsec.grpc.examples.common;

import com.orientsec.grpc.registry.common.URL;

/**
 * URL解码
 *
 * @author sxp
 * @since 2019/1/7
 */
public class UrlDecode {
  public static void main(String[] args) {
    String str = "override%3a%2f%2f0.0.0.0%2fcom.orientsec.grpc.demo.helloworld.Greeter%3fcategory%3dconfigurators%26dynamic%3dfalse%26default.requests%3d888";
    System.out.println(URL.decode(str));
  }
}
