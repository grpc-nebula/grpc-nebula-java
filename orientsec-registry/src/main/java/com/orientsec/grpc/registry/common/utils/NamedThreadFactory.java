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
package com.orientsec.grpc.registry.common.utils;



import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义名称的线程工厂类
 *
 * @author heiden
 * @since 2018/3/15.
 */
public class NamedThreadFactory implements ThreadFactory {
  /**
   * 线程池计数器
   */
  private static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

  /**
   * 线程序号计数器
   */
  private final AtomicInteger mThreadNum = new AtomicInteger(1);

  /**
   * 线程名称的前缀
   */
  private final String mPrefix;

  /**
   * 是否为守护进程
   */
  private final boolean mDaemo;

  /**
   * 进程组
   */
  private final ThreadGroup mGroup;

  public NamedThreadFactory() {
    this("pool-" + POOL_SEQ.getAndIncrement(), false);
  }

  /**
   * 指定名称前缀的线程工厂
   *
   * @author sxp
   * @since 2018/12/1
   */
  public NamedThreadFactory(String prefix) {
    this(prefix, false);
  }

  /**
   * 指定名称前缀并同时可以指定是否为守护进程的线程工厂
   *
   * @author sxp
   * @since 2018/12/1
   */
  public NamedThreadFactory(String prefix, boolean daemo) {
    mPrefix = prefix + "-thread-";
    mDaemo = daemo;
    SecurityManager s = System.getSecurityManager();
    mGroup = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
  }

  /**
   * 创建指定格式名称的线程
   *
   * @author sxp
   * @since 2018/12/1
   */
  public Thread newThread(Runnable runnable) {
    String name = mPrefix + mThreadNum.getAndIncrement();
    Thread ret = new Thread(mGroup, runnable, name, 0);
    ret.setDaemon(mDaemo);
    return ret;
  }

  /**
   * 获取线程组对象
   *
   * @author sxp
   * @since 2018/12/1
   */
  public ThreadGroup getThreadGroup() {
    return mGroup;
  }
}
