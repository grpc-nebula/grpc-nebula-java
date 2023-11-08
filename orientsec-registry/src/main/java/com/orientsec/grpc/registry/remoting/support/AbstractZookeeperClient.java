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


package com.orientsec.grpc.registry.remoting.support;

import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.registry.remoting.ChildListener;
import com.orientsec.grpc.registry.remoting.StateListener;
import com.orientsec.grpc.registry.remoting.ZookeeperClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;



/**
 * Created by heiden on 2017/3/15.
 * @since  modify by wjw 2017/4/14 change Logger from  org.slf4j.Logger to java.util.logging.Logger
 */
public abstract class AbstractZookeeperClient<TargetChildListener> implements ZookeeperClient {

  protected static final Logger logger = LoggerFactory.getLogger(AbstractZookeeperClient.class);

  private final URL url;

  private final Set<StateListener> stateListeners = new CopyOnWriteArraySet<StateListener>();

  private final ConcurrentMap<String, ConcurrentMap<ChildListener, TargetChildListener>> childListeners = new ConcurrentHashMap<String, ConcurrentMap<ChildListener, TargetChildListener>>();

  private volatile boolean closed = false;

  public AbstractZookeeperClient(URL url) {
    this.url = url;
  }

  public URL getUrl() {
    return url;
  }

  /**
   * @since 2019-08-12 modify by sxp 解决父节点有权限控制，子节点无权限控制，且父节点已经存在的情况下服务无法注册的问题
   */
  public void create(String path, boolean ephemeral) {
    int i = path.lastIndexOf('/');
    if (i > 0) {
      String parent = path.substring(0, i);
      if (!isNodeExists(parent)) {
        create(parent, false);
      }
    }

    if (ephemeral) {
      createEphemeral(path);
    } else {
      createPersistent(path);
    }
  }

  public void addStateListener(StateListener listener) {
    stateListeners.add(listener);
  }

  public void removeStateListener(StateListener listener) {
    stateListeners.remove(listener);
  }

  public Set<StateListener> getSessionListeners() {
    return stateListeners;
  }

  public List<String> addChildListener(String path, final ChildListener listener) {
    ConcurrentMap<ChildListener, TargetChildListener> listeners = childListeners.get(path);
    if (listeners == null) {
      childListeners.putIfAbsent(path, new ConcurrentHashMap<ChildListener, TargetChildListener>());
      listeners = childListeners.get(path);
    }
    TargetChildListener targetListener = listeners.get(listener);
    if (targetListener == null) {
      listeners.putIfAbsent(listener, createTargetChildListener(path, listener));
      targetListener = listeners.get(listener);
    }
    return addTargetChildListener(path, targetListener);
  }

  public void removeChildListener(String path, ChildListener listener) {
    ConcurrentMap<ChildListener, TargetChildListener> listeners = childListeners.get(path);
    if (listeners != null) {
      TargetChildListener targetListener = listeners.remove(listener);
      if (targetListener != null) {
        removeTargetChildListener(path, targetListener);
      }
    }
  }

  protected void stateChanged(int state) {
    for (StateListener sessionListener : getSessionListeners()) {
      sessionListener.stateChanged(state);
    }
  }

  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      doClose();
    } catch (Throwable t) {
      logger.warn(t.getMessage(), t);
    }
  }

  public String getData(String path){
    try {
      return doGetData(path);
    } catch (Throwable t) {
      logger.error( t.getMessage(), t);
    }
    return null;
  }

  protected abstract void doClose();

  protected abstract void createPersistent(String path);

  protected abstract void createEphemeral(String path);

  protected abstract TargetChildListener createTargetChildListener(String path, ChildListener listener);

  protected abstract List<String> addTargetChildListener(String path, TargetChildListener listener);

  protected abstract void removeTargetChildListener(String path, TargetChildListener listener);

  protected abstract String doGetData(String path);

  /**
   * 检查节点是否存在
   *
   * @author sxp
   * @since 2019/8/12
   */
  protected abstract boolean isNodeExists(String path);
}
