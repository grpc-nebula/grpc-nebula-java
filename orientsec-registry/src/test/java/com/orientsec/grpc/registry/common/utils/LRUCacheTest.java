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
package com.orientsec.grpc.registry.common.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for LRUCache
 *
 * @author sxp
 * @since 2018/11/29
 */
public class LRUCacheTest {
  @Test
  public void usage() throws Exception {
    int maxCapacity = 3;
    LRUCache cache = new LRUCache(maxCapacity);
    cache.put("1", 10);
    cache.put("2", 20);
    cache.put("3", 30);
    cache.put("4", 40);

    Assert.assertEquals(40, cache.get("4"));// 说明节点并没有删除
    Assert.assertEquals(maxCapacity, cache.size());
  }

  @Test
  public void removeEldestEntry() throws Exception {
    int maxCapacity = 3;
    LRUCache cache = new LRUCache(maxCapacity);
    cache.put("1", 10);
    cache.put("2", 20);
    cache.put("3", 30);

    Assert.assertFalse(cache.removeEldestEntry(null));

    cache.put("4", 40);
    //Assert.assertTrue(cache.removeEldestEntry(null));
    //这个方法始终返回false
    Assert.assertFalse(cache.removeEldestEntry(null));
  }

  @Test
  public void containsKey() throws Exception {
    int maxCapacity = 3;
    LRUCache cache = new LRUCache(maxCapacity);
    cache.put("1", 10);
    cache.put("2", 20);
    cache.put("3", 30);

    Assert.assertTrue(cache.containsKey("2"));
    Assert.assertFalse(cache.containsKey("4"));
  }


  @Test
  public void remove() throws Exception {
    int maxCapacity = 3;
    LRUCache cache = new LRUCache(maxCapacity);
    cache.put("1", 10);
    cache.put("2", 20);
    cache.put("3", 30);
    cache.put("4", 40);

    // 再次说明节点并没有删除
    Assert.assertEquals(40, cache.remove("4"));
  }


  @Test
  public void clear() throws Exception {
    int maxCapacity = 3;
    LRUCache cache = new LRUCache(maxCapacity);
    cache.put("1", 10);
    cache.put("2", 20);
    cache.put("3", 30);
    cache.put("4", 40);

    cache.clear();

    Assert.assertTrue(cache.isEmpty());
  }

  @Test
  public void testMaxCapacity() throws Exception {
    LRUCache cache = new LRUCache(3);
    cache.put("1", 10);
    cache.put("2", 20);
    cache.put("3", 30);
    cache.put("4", 40);

    Assert.assertEquals(3, cache.getMaxCapacity());

    cache.setMaxCapacity(4);
    Assert.assertEquals(4, cache.getMaxCapacity());
  }


}
