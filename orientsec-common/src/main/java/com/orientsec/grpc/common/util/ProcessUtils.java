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
package com.orientsec.grpc.common.util;

import java.lang.management.ManagementFactory;

/**
 * 进程工具类
 *
 * @author sxp
 * @since V1.0 Mar 24, 2017
 */
public final class ProcessUtils {

  /**
   * 获取当前进程的ID
   *
   * @author sxp
   * @since V1.0 Mar 24, 2017
   */
  public static String getProcessId() {
    // get name representing the running Java virtual machine.
    String name = ManagementFactory.getRuntimeMXBean().getName();

    String pid = name.split("@")[0];

    return pid;
  }

  public static long getPid() {
    return Long.parseLong(getProcessId());
  }

  /**
   * Test
   */
  public static void main(String[] args) {
    System.out.println(ProcessUtils.getProcessId());
  }

}
