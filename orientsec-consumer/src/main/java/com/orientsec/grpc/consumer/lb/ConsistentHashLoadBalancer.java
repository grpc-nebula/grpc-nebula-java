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
package com.orientsec.grpc.consumer.lb;

import com.google.common.base.Preconditions;
import com.orientsec.grpc.consumer.model.ServiceProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 一致性Hash算法
 * <p>
 * <pre>
 * 一致性Hash，相同参数的请求总是发到同一服务提供者。
 * 当被选中的服务提供者下线时，原本发往该服务提供者的请求，发送给其它服务提供者。
 *
 * 使用场景：
 * 服务提供者(P1、P2、P3...)系统对外提供API执行一些后台任务。
 * 例如，服务消费者C1调用服务启动任务、停止任务，在调用服务时，会传给服务端“任务的唯一标识jobId”。
 * 如果是调用P1启动任务，那么停止服务也必须调用P1，因为那个指定jobId的任务运行在P1所在的服务器。
 * 在这个场景中，需要对jobId计算散列值，然后根据散列值选择服务提供者。对于相同的jobId，要求计算出来
 * 的散列值相同，这样才能保证“相同参数的请求总是发到同一服务提供者”。
 * </pre>
 *
 * @author sxp
 * @since 2018/10/19
 */
public class ConsistentHashLoadBalancer {
  /**
   * key值为服务名称
   */
  private static final ConcurrentMap<String, ConsistentHashSelector> selectors
          = new ConcurrentHashMap<String, ConsistentHashSelector>();

  /**
   * 选择服务提供者
   *
   * @param serviceProviderMap 服务提供者集合
   * @param serviceName 服务名称
   * @param argument 参数
   * @author sxp
   * @since 2018/12/1
   */
  public static Map<String, ServiceProvider> chooseProvider(Map<String, ServiceProvider> serviceProviderMap, String serviceName, Object argument) {
    if (serviceProviderMap == null || serviceProviderMap.isEmpty() || serviceProviderMap.size() == 1) {
      return serviceProviderMap;
    }

    String arg;
    if (argument instanceof String) {
      arg = (String) argument;
    } else {
      arg = String.valueOf(argument);
    }

    // 通过identityHashCode判断服务列表是否发生变化，如果发生变化，需要重新构造选择器
    int identityHashCode = System.identityHashCode(serviceProviderMap);
    ConsistentHashSelector selector = selectors.get(serviceName);
    if (selector == null || selector.getIdentityHashCode() != identityHashCode) {
      selectors.put(serviceName, new ConsistentHashSelector(serviceProviderMap, identityHashCode));
      selector = selectors.get(serviceName);
    }

    Map<String, ServiceProvider> result = selector.select(arg);
    return result;
  }

  /**
   * 一致性Hash算法选择器
   * <p>
   * <pre>
   * 一致性Hash算法的资料
   * http://blog.csdn.net/cywosp/article/details/23397179/
   * http://www.cnblogs.com/hapjin/p/4737207.html
   * com.alibaba.dubbo.rpc.cluster.loadbalance.ConsistentHashLoadBalance
   * </pre>
   *
   * @author sxp
   * @since 2018/10/20
   */
  private static final class ConsistentHashSelector {
    // 节点的复制因子，虚拟节点个数 = 实际节点个数 * REPLICA_NUMBER，目的是增加算法的平衡性
    private final int REPLICA_NUMBER = 160;

    private final int identityHashCode;

    // 虚拟节点的hash值到真实节点的映射
    private final SortedMap<Long, Map.Entry<String, ServiceProvider>> circle
            = new TreeMap<Long, Map.Entry<String, ServiceProvider>>();

    public ConsistentHashSelector(Map<String, ServiceProvider> serviceProviderMap, int identityHashCode) {
      this.identityHashCode = identityHashCode;

      Set<Map.Entry<String, ServiceProvider>> entries = serviceProviderMap.entrySet();
      Preconditions.checkArgument(entries.size() > 0, "serviceProviderMap的大小不能等于0");

      for (Map.Entry<String, ServiceProvider> entry : entries) {
        add(entry);
      }
    }

    /**
     * 将服务提供者条件到long值做键值的集合中
     *
     * @author sxp
     * @since 2018/12/1
     */
    private void add(Map.Entry<String, ServiceProvider> node) {
      String key;

      for (int i = 0; i < REPLICA_NUMBER; i++) {
        key = node.getKey() + "#" + i;// ip:port#0, ip:port#1, ...
        circle.put(md5Hash(key), node);
      }
    }

    /**
     * 将字符串进行MD5散列并转化为长整数
     *
     * @author sxp
     * @since 2018/12/1
     */
    private long md5Hash(String key) {
      try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(key.getBytes());// 128bit, 16byte

        // 取值范围：0 ~ 2^32-1
        long result = (((long) (digest[3] & 0xFF) << 24)
                | ((long) (digest[2] & 0xFF) << 16)
                | ((long) (digest[1] & 0xFF) << 8)
                | (digest[0] & 0xFF))
                & 0xFFFFFFFFL;

        return result;
      } catch (NoSuchAlgorithmException e) {
        return 1L;
      }
    }

    /**
     * 根据参数值选择指定的服务提供者
     *
     * @author sxp
     * @since 2018/10/20
     */
    public Map<String, ServiceProvider> select(String arg) {
      long hash = md5Hash(arg);

      // 数据映射在两台虚拟机器所在环之间，按顺时针方向寻找机器
      if (!circle.containsKey(hash)) {
        SortedMap<Long, Map.Entry<String, ServiceProvider>> tailMap = circle.tailMap(hash);
        hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
      }

      Map.Entry<String, ServiceProvider> providerEntry = circle.get(hash);

      Map<String, ServiceProvider> result = new ConcurrentHashMap<String, ServiceProvider>();
      result.put(providerEntry.getKey(), providerEntry.getValue());

      return result;
    }

    /**
     * 获取身份Hash值
     *
     * @author sxp
     * @since 2018/12/1
     */
    public int getIdentityHashCode() {
      return identityHashCode;
    }

  }
}
