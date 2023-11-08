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

/**
 * 东方证券通用唯一识别码生成器
 * <p>
 * 从UUIDGenerator中独立出来，因为初始化大小为200个int的数组对性能影响很大！
 * <p/>
 *
 * @author sxp
 * @since 2018/8/3
 */
public class OrientsecUUIDGenerator {
  // 进程ID + 本机IP == > 转化为数字
  private static long PID_LOCAL_IP_NUM = (ProcessUtils.getPid() << 4) + IpUtils.getIpNum(IpUtils.getIP4WithPriority());

  // 暂时设置为最多支持200个子线程
  private static final int MAX_THREAD_COUNT = 200;
  private static final int[] countList = new int[MAX_THREAD_COUNT];

  /**
   * 基于东方证券grpc框架业务场景的UUID
   * <p>
   * 长度：36
   * 规则： (pid-IP) + 当前线程ID + 当前时间 + 计数器
   * <p/>
   *
   * @author sxp
   * @since 2018/8/3
   */
  public static String getGrpcUuid(long threadID, long currentTime) {
    if (threadID < MAX_THREAD_COUNT) {
      int index = (int) threadID;
      countList[index]++;

      char[] dstArray = new char[36];

      threadID = (threadID << 4) + PID_LOCAL_IP_NUM;
      currentTime = currentTime + (countList[index] << 4);

      NessUUID.digits(dstArray, 0, 8, threadID >> 32);
      dstArray[8] = '-';
      NessUUID.digits(dstArray, 9, 4, threadID >> 16);
      dstArray[13] = '-';
      NessUUID.digits(dstArray, 14, 4, threadID);
      dstArray[18] = '-';
      NessUUID.digits(dstArray, 19, 4, currentTime >> 48);
      dstArray[23] = '-';
      NessUUID.digits(dstArray, 24, 12, currentTime);

      return new String(dstArray);
    } else {
      // 超过MAX_THREAD_COUNT的就只用随机数
      return UUIDGenerator.newRandomUUID();
    }
  }

}
