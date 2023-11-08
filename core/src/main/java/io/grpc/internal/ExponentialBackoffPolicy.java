/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.internal;

import com.google.common.annotations.VisibleForTesting;
import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.resource.SystemConfig;
import com.orientsec.grpc.common.util.PropertiesUtils;

import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Retry Policy for Transport reconnection.  Initial parameters from
 * https://github.com/grpc/grpc/blob/master/doc/connection-backoff.md
 *
 * <p>TODO(carl-mastrangelo): add unit tests for this class
 */
public final class ExponentialBackoffPolicy implements BackoffPolicy {
  public static final class Provider implements BackoffPolicy.Provider {
    @Override
    public BackoffPolicy get() {
      return new ExponentialBackoffPolicy();
    }
  }

  private Random random = new Random();

  // 第一次失败重试前等待的时间
  private long initialBackoffNanos = TimeUnit.SECONDS.toNanos(getInitial());

  // 失败重试等待时间上限
  private long maxBackoffNanos = TimeUnit.SECONDS.toNanos(getMax());

  // 下一次失败重试等待时间乘以的倍数
  private double multiplier = getMultiplier();

  // 随机抖动因子
  private double jitter = getJitter();

  private long nextBackoffNanos = initialBackoffNanos;

  /**
   * grpc断线重连指数回退协议"第一次失败重试前等待的时间"参数
   * <p>
   * 类型long,缺省值1,单位秒
   * </p>
   *
   * @author sxp
   * @since 2019/2/11
   */
  private static long getInitial() {
    String key = GlobalConstants.Consumer.Key.BACKOFF_INITIAL;
    long defaultValue = 1L;
    Properties properties = SystemConfig.getProperties();

    long value = PropertiesUtils.getValidLongValue(properties, key, defaultValue);
    if (value <= 0) {
      value = defaultValue;
    }

    return value;
  }

  /**
   * grpc断线重连指数回退协议"失败重试等待时间上限"参数
   * <p>
   * 类型long,缺省值20,单位秒
   * </p>
   *
   * @author sxp
   * @since 2019/2/11
   */
  private static long getMax() {
    String key = GlobalConstants.Consumer.Key.BACKOFF_MAX;
    long defaultValue = 20L;
    Properties properties = SystemConfig.getProperties();

    long value = PropertiesUtils.getValidLongValue(properties, key, defaultValue);
    if (value <= 0) {
      value = defaultValue;
    }

    return value;
  }

  /**
   * grpc断线重连指数回退协议"下一次失败重试等待时间乘以的倍数"参数
   * <p>
   * 类型double,缺省值1.6,取值范围:大于1
   * </p>
   *
   * @author sxp
   * @since 2019/2/11
   */
  private static double getMultiplier() {
    String key = GlobalConstants.Consumer.Key.BACKOFF_MULTIPLIER;
    double defaultValue = 1.6;
    Properties properties = SystemConfig.getProperties();

    double value = PropertiesUtils.getValidDoubleValue(properties, key, defaultValue);
    if (value <= 1) {
      value = defaultValue;
    }

    return value;
  }

  /**
   * grpc断线重连指数回退协议"随机抖动因子"参数
   * <p>
   * 类型double,缺省值0.2,取值范围:大于0小于1
   * </p>
   *
   * @author sxp
   * @since 2019/2/11
   */
  private static double getJitter() {
    String key = GlobalConstants.Consumer.Key.BACKOFF_JITTER;
    double defaultValue = 0.2;
    Properties properties = SystemConfig.getProperties();

    double value = PropertiesUtils.getValidDoubleValue(properties, key, defaultValue);
    if (value <=0 || value >= 1) {
      value = defaultValue;
    }

    return value;
  }

  @Override
  public long nextBackoffNanos() {
    long currentBackoffNanos = nextBackoffNanos;
    nextBackoffNanos = Math.min((long) (currentBackoffNanos * multiplier), maxBackoffNanos);
    long delay = currentBackoffNanos
            + uniformRandom(-jitter * currentBackoffNanos, jitter * currentBackoffNanos);
    if (delay <= 0) {
      delay = 1;
    }
    return delay;
  }

  private long uniformRandom(double low, double high) {
    checkArgument(high >= low);
    double mag = high - low;
    return (long) (random.nextDouble() * mag + low);
  }

  /*
   * No guice and no flags means we get to implement these setters for testing ourselves.  Do not
   * call these from non-test code.
   */

  @VisibleForTesting
  ExponentialBackoffPolicy setRandom(Random random) {
    this.random = random;
    return this;
  }

  @VisibleForTesting
  ExponentialBackoffPolicy setInitialBackoffNanos(long initialBackoffNanos) {
    this.initialBackoffNanos = initialBackoffNanos;
    return this;
  }

  @VisibleForTesting
  ExponentialBackoffPolicy setMaxBackoffNanos(long maxBackoffNanos) {
    this.maxBackoffNanos = maxBackoffNanos;
    return this;
  }

  @VisibleForTesting
  ExponentialBackoffPolicy setMultiplier(double multiplier) {
    this.multiplier = multiplier;
    return this;
  }

  @VisibleForTesting
  ExponentialBackoffPolicy setJitter(double jitter) {
    this.jitter = jitter;
    return this;
  }
}

