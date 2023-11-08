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



import java.util.LinkedHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 最近最少使用的（least recently used）的缓存
 * <p>
 * 当缓存容量达到上限后，最先放入内存的变量最新被删除
 * </p>
 *
 * @author heiden
 * @since 2018/3/15.
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
  private static final long serialVersionUID = -5167631809472116969L;

  /**
   * 散列表加载因子
   */
  private static final float DEFAULT_LOAD_FACTOR = 0.75f;

  /**
   * 默认容量
   */
  private static final int DEFAULT_MAX_CAPACITY = 1000;

  /**
   * 当前容量
   */
  private volatile int maxCapacity;

  /**
   * Reentrant锁
   */
  private final Lock lock = new ReentrantLock();

  /**
   * 创建对象实例时，如果未指定容量，使用默认容量
   *
   * @author sxp
   * @since 2018/12/1
   */
  public LRUCache() {
    this(DEFAULT_MAX_CAPACITY);
  }

  /**
   * 创建指定容量的缓存
   *
   * @author sxp
   * @since 2018/12/1
   */
  public LRUCache(int maxCapacity) {
    super(16, DEFAULT_LOAD_FACTOR, true);
    this.maxCapacity = maxCapacity;
  }

  /**
   * 删除节点的条件
   *
   * @author sxp
   * @since 2018/12/1
   */
  @Override
  protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
    return size() > maxCapacity;
  }

  /**
   * 检测制定的key值是否存在
   *
   * @author sxp
   * @since 2018/12/1
   */
  @Override
  public boolean containsKey(Object key) {
    try {
      lock.lock();
      return super.containsKey(key);
    } finally {
      lock.unlock();
    }
  }

  /**
   * 根据key获取value
   *
   * @author sxp
   * @since 2018/12/1
   */
  @Override
  public V get(Object key) {
    try {
      lock.lock();
      return super.get(key);
    } finally {
      lock.unlock();
    }
  }

  /**
   * 向缓存中增加数据
   *
   * @author sxp
   * @since 2018/12/1
   */
  @Override
  public V put(K key, V value) {
    try {
      lock.lock();
      return super.put(key, value);
    } finally {
      lock.unlock();
    }
  }

  /**
   * 从缓存中删除指定key值的键值对
   *
   * @author sxp
   * @since 2018/12/1
   */
  @Override
  public V remove(Object key) {
    try {
      lock.lock();
      return super.remove(key);
    } finally {
      lock.unlock();
    }
  }

  /**
   * 缓存目前的大小
   *
   * @author sxp
   * @since 2018/12/1
   */
  @Override
  public int size() {
    try {
      lock.lock();
      return super.size();
    } finally {
      lock.unlock();
    }
  }

  /**
   * 清空缓存中的所有数据
   *
   * @author sxp
   * @since 2018/12/1
   */
  @Override
  public void clear() {
    try {
      lock.lock();
      super.clear();
    } finally {
      lock.unlock();
    }
  }

  /**
   * 获取缓存的最大容量
   *
   * @author sxp
   * @since 2018/12/1
   */
  public int getMaxCapacity() {
    return maxCapacity;
  }

  /**
   * 设置缓存的最大容量
   *
   * @author sxp
   * @since 2018/12/1
   */
  public void setMaxCapacity(int maxCapacity) {
    this.maxCapacity = maxCapacity;
  }

}
