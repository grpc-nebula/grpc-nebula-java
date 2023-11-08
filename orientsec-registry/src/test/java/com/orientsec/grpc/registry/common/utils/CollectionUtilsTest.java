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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test for CollectionUtils
 *
 * @author sxp
 * @since 2018/11/29
 */
public class CollectionUtilsTest {
  @Test
  public void sort() throws Exception {
    List<String> names = null;
    Assert.assertTrue(CollectionUtils.sort(names) == null);

    names = new ArrayList<String>(2);
    names.add("Dog");
    names.add("Alice");
    Assert.assertEquals("Alice", CollectionUtils.sort(names).get(0));

    names.add("Bob");
    Assert.assertEquals("Dog", CollectionUtils.sort(names).get(2));
  }

  @Test
  public void sortSimpleName() throws Exception {
    List<String> names = null;
    Assert.assertTrue(CollectionUtils.sortSimpleName(names) == null);

    names = new ArrayList<String>(2);
    names.add("com.aaa.Dog");
    names.add("com.bbb.Alice");
    Assert.assertEquals("com.bbb.Alice", CollectionUtils.sortSimpleName(names).get(0));
  }

  @Test
  public void splitAll() throws Exception {
    String separator = ":";
    Map<String, List<String>> list = null;

    Map<String, Map<String, String>> result = CollectionUtils.splitAll(list, separator);
    Assert.assertTrue(result == null);

    list = new HashMap<String, List<String>>();

    List<String> ages = new ArrayList<String>();
    ages.add("Alice:China");
    ages.add("Bob:America");

    list.put("nationality", ages);

    result = CollectionUtils.splitAll(list, separator);
    Assert.assertEquals("China", result.get("nationality").get("Alice"));
    Assert.assertEquals("America", result.get("nationality").get("Bob"));
  }

  @Test
  public void joinAll() throws Exception {
    String separator = ":";
    Map<String, Map<String, String>> map = null;

    Map<String, List<String>> result = CollectionUtils.joinAll(map, separator);
    Assert.assertTrue(result == null);

    map = new HashMap<String, Map<String, String>>(1);
    Map<String, String> nationalitys = new HashMap<String, String>(2);
    nationalitys.put("Alice", "China");
    nationalitys.put("Bob", "America");
    map.put("nationality", nationalitys);

    result = CollectionUtils.joinAll(map, separator);
    Assert.assertTrue(result.get("nationality").contains("Alice:China"));
    Assert.assertTrue(result.get("nationality").contains("Bob:America"));
  }

  @Test
  public void mapEquals() throws Exception {
    Map<String, Integer> mapA = null;
    Map<String, Integer> mapB = null;
    Assert.assertTrue(CollectionUtils.mapEquals(mapA, mapB));

    mapA = new ConcurrentHashMap<String, Integer>();
    Assert.assertFalse(CollectionUtils.mapEquals(mapA, mapB));

    mapB = new Hashtable<String, Integer>();
    Assert.assertTrue(CollectionUtils.mapEquals(mapA, mapB));

    mapA.put("sxp", 18);
    Assert.assertFalse(CollectionUtils.mapEquals(mapA, mapB));

    mapB.put("wf", 19);
    Assert.assertFalse(CollectionUtils.mapEquals(mapA, mapB));

    mapB.put("sxp", 18);
    Assert.assertFalse(CollectionUtils.mapEquals(mapA, mapB));

    mapA.put("wf", 19);
    Assert.assertTrue(CollectionUtils.mapEquals(mapA, mapB));
  }

  @Test
  public void toStringMap() throws Exception {
    Map<String, String> result;
    String[] names = new String[0];

    result = CollectionUtils.toStringMap(names);
    Assert.assertTrue(result.size() == 0);

    try {
      CollectionUtils.toStringMap("sxp");
      Assert.fail();
    } catch (Exception e) {
      // expected
    }

    result = CollectionUtils.toStringMap("sxp", "Smart", "Alice", "Pretty", "Bob", "Handsome");
    Assert.assertTrue(result.size() == 3);
    Assert.assertTrue("Smart".equals(result.get("sxp")));
    Assert.assertTrue("Pretty".equals(result.get("Alice")));
  }

  @Test
  public void toMap() throws Exception {
    Map<String, String> result;
    String[] names = new String[0];

    result = CollectionUtils.toMap(names);
    Assert.assertTrue(result.size() == 0);

    try {
      CollectionUtils.toMap("sxp");
      Assert.fail();
    } catch (Exception e) {
      // expected
    }

    result = CollectionUtils.toMap("sxp", "Smart", "Alice", "Pretty", "Bob", "Handsome");
    Assert.assertTrue(result.size() == 3);
    Assert.assertTrue("Smart".equals(result.get("sxp")));
    Assert.assertTrue("Pretty".equals(result.get("Alice")));
  }

  @Test
  public void isEmpty() throws Exception {
      Assert.assertTrue(CollectionUtils.isEmpty(null));
      Assert.assertTrue(CollectionUtils.isEmpty(new HashSet<Object>()));
  }

  @Test
  public void isNotEmpty() throws Exception {
    Assert.assertFalse(CollectionUtils.isNotEmpty(null));
    Assert.assertFalse(CollectionUtils.isNotEmpty(new TreeSet<Object>()));

    Set<String> set = new TreeSet<String>();
    set.add("sxp");
    Assert.assertTrue(CollectionUtils.isNotEmpty(set));
  }

}
