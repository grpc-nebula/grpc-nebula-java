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

import com.orientsec.grpc.common.collect.ConcurrentHashSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

/**
 * Test for ConcurrentHashSet
 *
 * @author sxp
 * @since 2018/11/29
 */
public class ConcurrentHashSetTest {
  @Test
  public void iterator() throws Exception {
    ConcurrentHashSet<String> set = new ConcurrentHashSet<String>();
    set.add("sxp");
    set.add("lsc");
    set.add("dmw");

    Iterator it = set.iterator();

    int size = 0;
    while (it.hasNext()) {
      size++;
      it.next();
    }

    Assert.assertEquals(3, size);
  }

  @Test
  public void size() throws Exception {
    ConcurrentHashSet<String> set = new ConcurrentHashSet<String>(2);
    set.add("sxp");
    set.add("lsc");
    set.add("dmw");

    Assert.assertEquals(3, set.size());
  }

  @Test
  public void isEmpty() throws Exception {
    ConcurrentHashSet<String> set = new ConcurrentHashSet<String>();
    Assert.assertTrue(set.isEmpty());

    set.add("sxp");
    set.add("lsc");
    set.add("dmw");
    Assert.assertFalse(set.isEmpty());
  }

  @Test
  public void contains() throws Exception {
    ConcurrentHashSet<String> set = new ConcurrentHashSet<String>();
    set.add("sxp");
    set.add("lsc");
    set.add("dmw");

    Assert.assertTrue(set.contains("sxp"));
  }


  @Test
  public void remove() throws Exception {
    ConcurrentHashSet<String> set = new ConcurrentHashSet<String>();
    set.add("sxp");
    set.add("lsc");
    set.add("dmw");

    set.remove("lsc");
    Assert.assertEquals(2, set.size());
  }

  @Test
  public void clear() throws Exception {
    ConcurrentHashSet<String> set = new ConcurrentHashSet<String>();
    set.add("sxp");
    set.add("lsc");
    set.add("dmw");

    set.clear();
    Assert.assertEquals(0, set.size());
    Assert.assertTrue(set.isEmpty());
  }

}
