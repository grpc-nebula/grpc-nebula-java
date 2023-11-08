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


package com.orientsec.grpc.registry.remoting.curator;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.model.RegistryCenter;
import com.orientsec.grpc.common.resource.AllRegisterCenterConf;
import com.orientsec.grpc.common.resource.SystemConfig;
import com.orientsec.grpc.common.util.MathUtils;
import com.orientsec.grpc.common.util.PropertiesUtils;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.common.utils.StringUtils;
import com.orientsec.grpc.registry.remoting.ChildListener;
import com.orientsec.grpc.registry.remoting.StateListener;
import com.orientsec.grpc.registry.remoting.support.AbstractZookeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;


/**
 * Created by heiden on 2017/3/15.
 */
public class CuratorZookeeperClient extends AbstractZookeeperClient<CuratorWatcher> {
  private final static Logger logger = LoggerFactory.getLogger(CuratorZookeeperClient.class);

  private final CuratorFramework client;

  public CuratorZookeeperClient(URL url) {
    super(url);

    String urlId = url.getId();
    if (StringUtils.isEmpty(url.getId())) {
      throw new IllegalStateException("注册中心url的唯一标识不能为空");
    }

    // zk断线重连时，每3秒重连一次，直到连接上zk，或者超过根据设置的天数计算出的重试次数才停止
    int maxElapsedDays = getMaxElapsedDays();
    int sleepMsBetweenRetries = 3 * 1000;

    // 根据设置的天数计算获得重试次数
    int retryTime = getRetryTime(maxElapsedDays, sleepMsBetweenRetries);

    CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
            .connectString(url.getBackupAddress())
            .retryPolicy(new RetryNTimes(retryTime, sleepMsBetweenRetries))
            .connectionTimeoutMs(getConnectionTimeoutMs())
            .sessionTimeoutMs(getSessionTimeoutMs());

    ConcurrentMap<String, RegistryCenter> allConfMap = AllRegisterCenterConf.getAllConfMap();
    RegistryCenter rc = allConfMap.get(urlId);
    String userPassword = rc.getAclUserPwd();

    if (StringUtils.isNotEmpty(userPassword)) {
      byte[] auth = userPassword.getBytes(StandardCharsets.UTF_8);
      String scheme = ZkACLProvider.getScheme();
      builder = builder.aclProvider(new ZkACLProvider(userPassword)).authorization(scheme, auth);
    }

    client = builder.build();
    client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
      public void stateChanged(CuratorFramework client, ConnectionState state) {
        if (state == ConnectionState.LOST) {
          CuratorZookeeperClient.this.stateChanged(StateListener.DISCONNECTED);
        } else if (state == ConnectionState.CONNECTED) {
          CuratorZookeeperClient.this.stateChanged(StateListener.CONNECTED);
        } else if (state == ConnectionState.RECONNECTED) {
          CuratorZookeeperClient.this.stateChanged(StateListener.RECONNECTED);
        }
      }
    });
    client.start();
  }

  /**
   * @since 2019-11-21 modify by wlh 修改ZK重连时间，缺省由原先1天修改为30天，单位由原先毫秒值修改为天
   */
  private static int getMaxElapsedDays() {
    String key = GlobalConstants.REGISTRY_RETRY_TIME;

    // 缺省值为30天
    int defaultValue = 30;
    Properties properties = SystemConfig.getProperties();

    int value = PropertiesUtils.getValidIntegerValue(properties, key, defaultValue);
    if (value <= 0) {
      value = defaultValue;
    }

    return value;
  }

  /**
   * @since 2019-06-26 modify by sxp 为了消除curator框架的警告日志，将默认连接超时时间调整为4秒
   */
  private static int getConnectionTimeoutMs() {
    String key = GlobalConstants.REGISTRY_CONNECTIONTIMEOUT;
    int defaultValue = 4000;
    Properties properties = SystemConfig.getProperties();

    int value = PropertiesUtils.getValidIntegerValue(properties, key, defaultValue);
    if (value <= 0 || value < 100) {
      value = defaultValue;
    }

    return value;
  }

  /**
   * 根据天数，计算重试次数
   *
   * @param days
   * @param sleepMsBetweenRetries
   * @return
   * @since 2019-11-21 created by wlh
   */
  private static int getRetryTime(int days, int sleepMsBetweenRetries){
    /**
     * 1. 根据天数换算毫秒值
     * 2. 天数毫秒值 / sleepMsBetweenRetries = 重试次数
     * 3. 计算后的值四舍五入后返回
     */
    double daysTimeMillis = (double) days * 24 * 60 * 60 * 1000;
    int retryTime = (int) MathUtils.round(daysTimeMillis / sleepMsBetweenRetries, 0);
    if (retryTime > 0) {
      return retryTime;
    } else {
      return Integer.MAX_VALUE;
    }

  }

  private static int getSessionTimeoutMs() {
    String key = GlobalConstants.REGISTRY_SESSIONTIMEOUT;
    int defaultValue = 4000;
    Properties properties = SystemConfig.getProperties();

    int value = PropertiesUtils.getValidIntegerValue(properties, key, defaultValue);
    if (value <= 0 || value < 100) {
      value = defaultValue;
    }

    return value;
  }

  public void createPersistent(String path) {
    try {
      client.create().forPath(path);
    } catch (KeeperException.NodeExistsException e) {
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  public void createEphemeral(String path) {
    try {
      client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
    } catch (KeeperException.NodeExistsException e) {
      // 已存在的节点可能是即将过期的节点，删除并重建该节点
      delete(path);
      createEphemeral(path);
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  public void delete(String path) {
    try {
      client.delete().forPath(path);
    } catch (KeeperException.NoNodeException e) {
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  public List<String> getChildren(String path) {
    try {
      return client.getChildren().forPath(path);
    } catch (KeeperException.NoNodeException e) {
      return null;
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  public String doGetData(String path){
    try {
      return new String(client.getData().forPath(path));
    } catch (KeeperException.NoNodeException e) {
      return null;
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  public boolean isNodeExists(String path){
    try {
      Stat stat = client.checkExists().forPath(path);
      return (stat != null) ? true : false;
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  public boolean isConnected() {
    return client.getZookeeperClient().isConnected();
  }

  public void doClose() {
    client.close();
  }

  private class CuratorWatcherImpl implements CuratorWatcher {

    private volatile ChildListener listener;

    public CuratorWatcherImpl(ChildListener listener) {
      this.listener = listener;
    }

    public void unwatch() {
      this.listener = null;
    }

    /**
     * @since 2019-3-1 modify by sxp 忽略zookeeper的WatchedEvent中path为空的情况
     */
    @Override
    public void process(WatchedEvent event) throws Exception {
      String path = event.getPath();
      if (StringUtils.isEmpty(path)) {
        return;
      }

      if (listener != null) {
        listener.childChanged(path, client.getChildren().usingWatcher(this).forPath(path));
      }
    }
  }

  public CuratorWatcher createTargetChildListener(String path, ChildListener listener) {
    return new CuratorWatcherImpl(listener);
  }

  public List<String> addTargetChildListener(String path, CuratorWatcher listener) {
    try {
      return client.getChildren().usingWatcher(listener).forPath(path);
    } catch (KeeperException.NoNodeException e) {
      return null;
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  public void removeTargetChildListener(String path, CuratorWatcher listener) {
    ((CuratorWatcherImpl) listener).unwatch();
  }

}
