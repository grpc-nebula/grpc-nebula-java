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
package com.orientsec.grpc.provider.qos;

import com.orientsec.grpc.common.constant.GlobalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;



/**
 * 服务端请求数控制器
 *
 * @author sxp
 * @since V1.0 2017/3/29
 */
public class RequestsController {
  private static final Logger logger = LoggerFactory.getLogger(RequestsController.class);

  /**
   * 不限制请求数的配置值
   */
  public static final int NO_LIMIT_NUM = 0;

  /**
   * 服务的最大请求数，即同一时刻最多可以处理多少个客户端请求
   */
  private volatile int max = NO_LIMIT_NUM;

  /**
   * 服务当前的请求数，即当前服务端正在处理多少个客户端请求
   * <p>
   * 为了实现自增和自减的原子操作，使用AtomicInteger对象
   * <p/>
   */
  private volatile AtomicInteger current = new AtomicInteger(0);

  public RequestsController(int max) {
    max = getValidMax(max);
    this.max = max;
  }

  /**
   * 获得有效的最大请求数
   * <p>
   * 防止出现参数值小于0的情况
   * </p>
   *
   * @author sxp
   */
  public static int getValidMax(int max) {
    if (max < 0) {
      max = GlobalConstants.Provider.DEFAULT_REQUESTS_NUM;
    }
    return max;
  }

  /**
   * 获取最大的请求数
   *
   * @author sxp
   * @since 2018/12/1
   */
  public int getMax() {
    return max;
  }

  /**
   * 不会出现多个线程同时修改max，因此这里不加同步控制
   *
   * @author sxp
   */
  public void setMax(int max) {
    max = getValidMax(max);

    this.max = max;
  }

  /**
   * 获取当前请求数
   *
   * @author sxp
   * @since 2018/12/1
   */
  public int getCurrent() {
    return current.get();
  }

  /**
   * 当前请求数加1
   *
   * @author sxp
   */
  public boolean increase() {
    if (current.get() >= max) {
      // 修改max会出现current>max的情况
      return false;
    }

    current.incrementAndGet();
    return true;
  }

  /**
   * 当前请求数减1
   *
   * @author sxp
   */
  public void decrease() {
    long currentNum = current.get();

    if (currentNum == 0) {
      // ServerCallImpl.ServerStreamListenerImpl.closeRequest方法最终也会调用当前方法
      // 而调用closeRequest方法之前，有可能没有调用当前类increase()
      return;
    } else if (currentNum < 0) {
      current.set(0);
      logger.warn("调用decrease方法时，服务提供者的当前请求数为" + currentNum);
      return;
    }

    current.decrementAndGet();
  }

  /**
   * 检验当前服务接口的请求数是否满足条件
   * <p>
   * 即当前接口的请求数是否已经达到允许的最大值
   * </p>
   *
   * @author sxp
   * @since V1.0 2017-3-29
   */
  public boolean checkRequests() {
    if (max == NO_LIMIT_NUM) {
      return true;
    }
    return (current.get() < max);
  }

  @Override
  public String toString() {
    return "RequestsController{" +
            "max=" + max +
            ", current=" + current.get() +
            '}';
  }
}
