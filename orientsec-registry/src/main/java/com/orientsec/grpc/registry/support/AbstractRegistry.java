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


package com.orientsec.grpc.registry.support;

import com.orientsec.grpc.registry.NotifyListener;
import com.orientsec.grpc.registry.Registry;
import com.orientsec.grpc.registry.common.Constants;
import com.orientsec.grpc.registry.common.URL;
import com.orientsec.grpc.common.collect.ConcurrentHashSet;
import com.orientsec.grpc.registry.common.utils.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by heiden on 2017/3/15.
 *
 * @since 2019-1-29 modify by sxp change java.util.logging.Logger to org.slf4j.Logger
 */
public abstract class AbstractRegistry implements Registry {
  private static final Logger logger = LoggerFactory.getLogger(AbstractRegistry.class);

  // URL地址分隔符，用于文件缓存中，服务提供者URL分隔
  private static final char URL_SEPARATOR = ' ';

  // URL地址分隔正则表达式，用于解析文件缓存中服务提供者URL列表
  private static final String URL_SPLIT = "\\s+";

  private URL registryUrl;

  private final Set<URL> registered = new ConcurrentHashSet<URL>();

  private final ConcurrentMap<URL, Set<NotifyListener>> subscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>();

  private final ConcurrentMap<URL, Map<String, List<URL>>> notified = new ConcurrentHashMap<URL, Map<String, List<URL>>>();

  public AbstractRegistry(URL url) {
    setUrl(url);
    notify(url.getBackupUrls());
  }

  protected void setUrl(URL url) {
    if (url == null) {
      throw new IllegalArgumentException("registry url == null");
    }
    this.registryUrl = url;
  }

  public URL getUrl() {
    return registryUrl;
  }

  public Set<URL> getRegistered() {
    return registered;
  }

  public Map<URL, Set<NotifyListener>> getSubscribed() {
    return subscribed;
  }

  public Map<URL, Map<String, List<URL>>> getNotified() {
    return notified;
  }

  public List<URL> lookup(URL url) {
    List<URL> result = new ArrayList<URL>();
    Map<String, List<URL>> notifiedUrls = getNotified().get(url);
    if (notifiedUrls != null && notifiedUrls.size() > 0) {
      for (List<URL> urls : notifiedUrls.values()) {
        for (URL u : urls) {
          if (!Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
            result.add(u);
          }
        }
      }
    } else {
      final AtomicReference<List<URL>> reference = new AtomicReference<List<URL>>();
      NotifyListener listener = new NotifyListener() {
        public void notify(List<URL> urls) {
          reference.set(urls);
        }
      };
      subscribe(url, listener); // 订阅逻辑保证第一次notify后再返回
      List<URL> urls = reference.get();
      if (urls != null && urls.size() > 0) {
        for (URL u : urls) {
          if (!Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
            result.add(u);
          }
        }
      }
    }
    return result;
  }

  public void register(URL url) {
    if (url == null) {
      throw new IllegalArgumentException("register url == null");
    }
    logger.debug("Register: " + url);
    registered.add(url);
  }

  public void unregister(URL url) {
    if (url == null) {
      throw new IllegalArgumentException("unregister url == null");
    }
    logger.debug("Unregister: " + url);
    registered.remove(url);
  }

  public void subscribe(URL url, NotifyListener listener) {
    if (url == null) {
      throw new IllegalArgumentException("subscribe url == null");
    }
    if (listener == null) {
      throw new IllegalArgumentException("subscribe listener == null");
    }
    logger.debug("Subscribe: " + url);
    Set<NotifyListener> listeners = subscribed.get(url);
    if (listeners == null) {
      subscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
      listeners = subscribed.get(url);
    }
    listeners.add(listener);
  }

  public void unsubscribe(URL url, NotifyListener listener) {
    if (url == null) {
      throw new IllegalArgumentException("unsubscribe url == null");
    }
    if (listener == null) {
      throw new IllegalArgumentException("unsubscribe listener == null");
    }
    logger.debug("Unsubscribe: " + url);
    Set<NotifyListener> listeners = subscribed.get(url);
    if (listeners != null) {
      listeners.remove(listener);
    }
  }

  protected void recover() throws Exception {
    // 这个方法理论上不会调用到
  }

  protected static List<URL> filterEmpty(URL url, List<URL> urls) {
    if (urls == null || urls.size() == 0) {
      List<URL> result = new ArrayList<URL>(1);
      result.add(url.setProtocol(Constants.EMPTY_PROTOCOL));
      return result;
    }
    return urls;
  }

  protected void notify(List<URL> urls) {
    if (urls == null || urls.isEmpty()) return;

    for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
      URL url = entry.getKey();

      if (!UrlUtils.isMatch(url, urls.get(0))) {
        continue;
      }

      Set<NotifyListener> listeners = entry.getValue();
      if (listeners != null) {
        for (NotifyListener listener : listeners) {
          try {
            notify(url, listener, filterEmpty(url, urls));
          } catch (Throwable t) {
            logger.error("Failed to notify registry event, urls: " + urls + ", cause: " + t.getMessage(), t);
          }
        }
      }
    }
  }

  protected void notify(URL url, NotifyListener listener, List<URL> urls) {
    if (url == null) {
      throw new IllegalArgumentException("notify url == null");
    }
    if (listener == null) {
      throw new IllegalArgumentException("notify listener == null");
    }
    if ((urls == null || urls.size() == 0)
            && !Constants.ANY_VALUE.equals(url.getServiceInterface())) {
      logger.warn("Ignore empty notify urls for subscribe url " + url);
      return;
    }
    logger.debug("Notify urls for subscribe url " + url + ", urls: " + urls);
    Map<String, List<URL>> result = new HashMap<String, List<URL>>();
    for (URL u : urls) {
      if (UrlUtils.isMatch(url, u)) {
        String category = u.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
        List<URL> categoryList = result.get(category);
        if (categoryList == null) {
          categoryList = new ArrayList<URL>();
          result.put(category, categoryList);
        }
        categoryList.add(u);
      }
    }
    if (result.size() == 0) {
      return;
    }
    Map<String, List<URL>> categoryNotified = notified.get(url);
    if (categoryNotified == null) {
      notified.putIfAbsent(url, new ConcurrentHashMap<String, List<URL>>());
      categoryNotified = notified.get(url);
    }
    for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
      String category = entry.getKey();
      List<URL> categoryList = entry.getValue();
      categoryNotified.put(category, categoryList);
      listener.notify(categoryList);
    }
  }

  public void destroy() {
    logger.debug("Destroy registry:" + getUrl());
    Set<URL> destroyRegistered = new HashSet<URL>(getRegistered());
    if (!destroyRegistered.isEmpty()) {
      for (URL url : new HashSet<URL>(getRegistered())) {
        if (url.getParameter(Constants.DYNAMIC_KEY, true)) {
          try {
            unregister(url);
            logger.debug("Destroy unregister url " + url);
          } catch (Throwable t) {
            logger.warn("Failed to unregister url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
          }
        }
      }
    }
    Map<URL, Set<NotifyListener>> destroySubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
    if (!destroySubscribed.isEmpty()) {
      for (Map.Entry<URL, Set<NotifyListener>> entry : destroySubscribed.entrySet()) {
        URL url = entry.getKey();
        for (NotifyListener listener : entry.getValue()) {
          try {
            unsubscribe(url, listener);
            logger.debug("Destroy unsubscribe url " + url);
          } catch (Throwable t) {
            logger.warn("Failed to unsubscribe url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
          }
        }
      }
    }
  }

  public String toString() {
    return getUrl().toString();
  }

}
