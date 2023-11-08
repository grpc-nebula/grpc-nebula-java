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

import io.grpc.Attributes;
import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.NameResolver;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.ConnectivityState.CONNECTING;
import static io.grpc.ConnectivityState.READY;
import static io.grpc.ConnectivityState.SHUTDOWN;
import static io.grpc.ConnectivityState.TRANSIENT_FAILURE;

/**
 * A {@link LoadBalancer} that provides no load-balancing over the addresses from the {@link
 * NameResolver}.  The channel's default behavior is used, which is walking down the address list
 * and sticking to the first that works.
 *
 * @since 2019.12.02 modify by sxp 支持subchannel的缓存
 */
final class PickFirstLoadBalancer extends LoadBalancer {
  private static final Logger logger = LoggerFactory.getLogger(PickFirstLoadBalancer.class);
  private final Helper helper;
  private final ConcurrentMap<EquivalentAddressGroup, Subchannel> subchannels = new ConcurrentHashMap<>();
  private Subchannel currentSubchannel;
  private volatile EquivalentAddressGroup currentAddressGroup;

  PickFirstLoadBalancer(Helper helper) {
    this.helper = checkNotNull(helper, "helper");
  }

  @Override
  public void handleResolvedAddressGroups(
      List<EquivalentAddressGroup> servers, Attributes attributes) {
    if (servers == null || servers.isEmpty()) {
      String errorMsg = "传入的servers参数不能为空";
      logger.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }

    EquivalentAddressGroup currentAddressGroup = servers.get(0);
    Subchannel subchannel = subchannels.get(currentAddressGroup);

    // 如果含有多个服务端地址，只考虑第一个
    if (servers.size() > 1) {
      servers = servers.subList(0, 1);
    }

    if (subchannel == null) {
      subchannel = helper.createSubchannel(servers, Attributes.EMPTY);
      Subchannel oldValue = subchannels.putIfAbsent(currentAddressGroup, subchannel);
      if (oldValue == null) {
        // The channel state does not get updated when doing name resolving today, so for the moment
        // let LB report CONNECTION and call subchannel.requestConnection() immediately.
        helper.updateBalancingState(CONNECTING, new Picker(PickResult.withSubchannel(subchannel)));
        subchannel.requestConnection();
      } else {
        subchannel = oldValue;
        helper.updateBalancingState(READY, new Picker(PickResult.withSubchannel(subchannel)));
      }
    } else {
      // helper.updateSubchannelAddresses(subchannel, servers);
      helper.updateBalancingState(READY, new Picker(PickResult.withSubchannel(subchannel)));
    }

    currentSubchannel = subchannel;
  }

  @Override
  public void handleNameResolutionError(Status error) {
    // NB(lukaszx0) Whether we should propagate the error unconditionally is arguable. It's fine
    // for time being.
    helper.updateBalancingState(TRANSIENT_FAILURE, new Picker(PickResult.withError(error)));
  }

  @Override
  public void handleSubchannelState(Subchannel subchannel, ConnectivityStateInfo stateInfo) {
    ConnectivityState currentState = stateInfo.getState();
    if (currentState == SHUTDOWN) {
      return;
    }

    EquivalentAddressGroup addressGroup = subchannel.getAddresses();
    Subchannel theSubchannel = subchannels.get(addressGroup);
    if (theSubchannel == null) {
      return;
    }

    if (theSubchannel != currentSubchannel) {
      return;
    }

    SubchannelPicker picker;
    switch (currentState) {
      case IDLE:
        picker = new RequestConnectionPicker(subchannel);
        break;
      case CONNECTING:
        // It's safe to use RequestConnectionPicker here, so when coming from IDLE we could leave
        // the current picker in-place. But ignoring the potential optimization is simpler.
        picker = new Picker(PickResult.withNoResult());
        break;
      case READY:
        picker = new Picker(PickResult.withSubchannel(subchannel));
        break;
      case TRANSIENT_FAILURE:
        picker = new Picker(PickResult.withError(stateInfo.getStatus()));
        break;
      default:
        throw new IllegalArgumentException("Unsupported state:" + currentState);
    }

    helper.updateBalancingState(currentState, picker);
  }

  @Override
  public void shutdown() {
    logger.info("正在关闭PickFirstLoadBalancer...");

    Set<Map.Entry<EquivalentAddressGroup, Subchannel>> set = subchannels.entrySet();
    Subchannel theSubchannel;

    for (Map.Entry<EquivalentAddressGroup, Subchannel> entry : set) {
      theSubchannel = entry.getValue();
      if (theSubchannel != null) {
        theSubchannel.shutdown();
      }
    }

    subchannels.clear();
  }

  /**
   * No-op picker which doesn't add any custom picking logic. It just passes already known result
   * received in constructor.
   */
  private static final class Picker extends SubchannelPicker {
    private final PickResult result;

    Picker(PickResult result) {
      this.result = checkNotNull(result, "result");
    }

    @Override
    public PickResult pickSubchannel(PickSubchannelArgs args) {
      return result;
    }
  }

  /** Picker that requests connection during pick, and returns noResult. */
  private static final class RequestConnectionPicker extends SubchannelPicker {
    private final Subchannel subchannel;

    RequestConnectionPicker(Subchannel subchannel) {
      this.subchannel = checkNotNull(subchannel, "subchannel");
    }

    @Override
    public PickResult pickSubchannel(PickSubchannelArgs args) {
      subchannel.requestConnection();
      return PickResult.withNoResult();
    }

    @Override
    public void requestConnection() {
      subchannel.requestConnection();
    }
  }

  /**
   * 获取服务端地址
   *
   * @author sxp
   * @since 2019/1/29
   * @since 2019/12/4 modify by sxp 根据currentSubchannel获得服务端地址
   */
  @Override
  public EquivalentAddressGroup getAddresses() {
    if (currentAddressGroup != null) {
      return currentAddressGroup;
    }
    if (currentSubchannel == null) {
      return null;
    }

    EquivalentAddressGroup addressGroup = currentSubchannel.getAddresses();
    return addressGroup;
  }

  /**
   * 设置当前服务端地址
   *
   * @author sxp
   * @since 2019/12/4
   */
  @Override
  public void setAddress(EquivalentAddressGroup addressGroup) {
    currentAddressGroup = addressGroup;
  }

  /**
   * 删除客户端与离线服务端之间的无效subchannel
   *
   * @author sxp
   * @since 2019/12/02
   */
  @Override
  public void removeInvalidCacheSubchannels(Set<String> removeHostPorts) {
    if (removeHostPorts == null || removeHostPorts.isEmpty()) {
      return;
    }

    Subchannel theSubchannel;
    EquivalentAddressGroup server;

    for (String hostAndPort: removeHostPorts) {
      server = getAddressGroupByHostAndPort(hostAndPort);
      if (server == null) {
        continue;
      }
      theSubchannel = subchannels.remove(server);
      if (theSubchannel != null) {
        logger.info("关闭" + server + "subchannel");
        theSubchannel.shutdown();
      }
    }
  }



}
