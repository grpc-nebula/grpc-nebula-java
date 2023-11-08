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

package io.grpc;

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.consumer.core.ConsumerServiceRegistry;
import com.orientsec.grpc.consumer.internal.ProvidersListener;
import com.orientsec.grpc.consumer.model.ServiceProvider;
import com.orientsec.grpc.registry.common.URL;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A pluggable component that resolves a target {@link URI} and return addresses to the caller.
 *
 * <p>A {@code NameResolver} uses the URI's scheme to determine whether it can resolve it, and uses
 * the components after the scheme for actual resolution.
 *
 * <p>The addresses and attributes of a target may be changed over time, thus the caller registers a
 * {@link Listener} to receive continuous updates.
 *
 * <p>A {@code NameResolver} does not need to automatically re-resolve on failure. Instead, the
 * {@link Listener} is responsible for eventually (after an appropriate backoff period) invoking
 * {@link #refresh()}.
 *
 * @since 1.0.0
 */
@ExperimentalApi("https://github.com/grpc/grpc-java/issues/1770")
@ThreadSafe
public abstract class NameResolver {
  /**
   * Returns the authority used to authenticate connections to servers.  It <strong>must</strong> be
   * from a trusted source, because if the authority is tampered with, RPCs may be sent to the
   * attackers which may leak sensitive user data.
   *
   * <p>An implementation must generate it without blocking, typically in line, and
   * <strong>must</strong> keep it unchanged. {@code NameResolver}s created from the same factory
   * with the same argument must return the same authority.
   *
   * @since 1.0.0
   */
  public abstract String getServiceAuthority();

  /**
   * Starts the resolution.
   *
   * @param listener used to receive updates on the target
   * @since 1.0.0
   */
  public abstract void start(Listener listener);

  /**
   * Stops the resolution. Updates to the Listener will stop.
   *
   * @since 1.0.0
   */
  public abstract void shutdown();

  /**
   * Re-resolve the name.
   *
   * <p>Can only be called after {@link #start} has been called.
   *
   * <p>This is only a hint. Implementation takes it as a signal but may not start resolution
   * immediately. It should never throw.
   *
   * <p>The default implementation is no-op.
   *
   * @since 1.0.0
   */
  public void refresh() {}

  /**
   * set the service name.
   *
   * @param serviceName used to provider service name for zk query
   */
  public void setServiceName(String serviceName) {
  }

  /**
   * set the service name.
   *
   * @param registry used to provider registry interface for zk query
   */
  public void setRegistry(ConsumerServiceRegistry registry) {
  }

  /**
   * return the registry ip info
   */
  public URL getURL() {
    return null;
  }

  /**
   * execute given init
   *
   */
  public NameResolver build() {
    return this;
  }

  public void registry() {
  }
  public int getProvidersCount() {
    return Integer.MAX_VALUE;
  }

  /**
   * 负载均衡重选服务器
   *
   * @Author yuanzhonglin
   * @since 2019/4/17
   */
  public void resolveServerInfo(Object argument, String method) {
  }

  /**
   * 获取客户端的注册地址ID
   *
   * @author sxp
   * @since 2018-6-21
   */
  public String getSubscribeId() {
    return null;
  }

  /**
   * 获取负载均衡使用的服务提供者备选列表
   *
   * @author sxp
   * @since 2018-6-21
   */
  public Map<String, ServiceProvider> getProvidersForLoadBalance() {
    return null;
  }

  /**
   * 获取负载均衡之后的服务器列表(只有一条数据)
   *
   * @author sxp
   * @since 2018-6-21
   */
  public Map<String, ServiceProvider> getServiceProviderMap() {
    return null;
  }

  /**
   * 获取服务列表监听器中的初始服务提供者列表
   *
   * @author sxp
   * @since 2018-6-21
   */
  public ProvidersListener getProvidersListener() {
    return null;
  }

  /**
   * 重新结算【经过负载均衡算法之后的服务提供者个数】
   *
   * @author sxp
   * @since 2018-8-31
   */
  public void reCalculateProvidersCountAfterLoadBalance(String method) {
  }

  /**
   * 获取服务名称
   *
   * @author sxp
   * @since 2018/10/19
   */
  public String getServiceName() {
    return null;
  }

  /**
   * 记录与之对应的ManagedChannel
   *
   * @author sxp
   * @since 2019/1/31
   */
  public void setManagedChannel(ManagedChannel mc) {
  }

  /**
   * 获得与之对应的ManagedChannel
   *
   * @author sxp
   * @since 2019/1/31
   */
  public ManagedChannel getManagedChannel() {
    return null;
  }

  /**
   * 获取当前客户端的负载均衡策略集合
   *
   * @Author yuanzhonglin
   * @since 2019/4/16
   */
  public Map<String, GlobalConstants.LB_STRATEGY> getLoadBlanceStrategyMap() {
    return null;
  }

  /**
   * 设置当前客户端的负载均衡策略集合
   *
   * @Author yuanzhonglin
   * @since 2019/4/16
   */
  public void setLoadBlanceStrategyMap(Map<String, GlobalConstants.LB_STRATEGY> map) {
  }

  /**
   * 给定的host:port是否在被过滤的服务器列表中
   *
   * @author sxp
   * @since 2019/3/1
   */
  public boolean isInfilteredProviders(String host, int port) {
    return false;
  }

  /**
   * 解析一个指定的服务端
   *
   * @author sxp
   * @since 2019/3/4
   */
  public boolean resolveOneServer(EquivalentAddressGroup server) {
    return false;
  }

  /**
   * 使用参数路由时，重新选择服务提供者
   *
   * @author zhuyujie
   * @since 2020-7-8
   */
  public boolean reselectProviderByParameterRouter(Map<String, Object> parameters, String method, Object argument) {
    return false;
  }

  public Map<String, ServiceProvider> getLastProviderMapAfterParamRoute() {
    return null;
  }

  public Map<String, ServiceProvider> getAllProviders() {
    return null;
  }

  /**
   * 服务端分组信息是否满足客户端
   *
   * @author sxp
   * @since 2019/11/14
   */
  public boolean isGroupValid(String host, int port) {
    return true;
  }

  /**
   * 删除客户端与离线服务端之间的无效subchannel
   *
   * @author sxp
   * @since 2019/12/02
   */
  public void removeInvalidCacheSubchannels(Set<String> removeHostPorts) {
  }

  /**
   * Factory that creates {@link NameResolver} instances.
   *
   * @since 1.0.0
   */
  public abstract static class Factory {
    /**
     * The port number used in case the target or the underlying naming system doesn't provide a
     * port number.
     *
     * @since 1.0.0
     */
    public static final Attributes.Key<Integer> PARAMS_DEFAULT_PORT =
        Attributes.Key.create("params-default-port");

    /**
     * Creates a {@link NameResolver} for the given target URI, or {@code null} if the given URI
     * cannot be resolved by this factory. The decision should be solely based on the scheme of the
     * URI.
     *
     * @param targetUri the target URI to be resolved, whose scheme must not be {@code null}
     * @param params optional parameters. Canonical keys are defined as {@code PARAMS_*} fields in
     *               {@link Factory}.
     * @since 1.0.0
     */
    @Nullable
    public abstract NameResolver newNameResolver(URI targetUri, Attributes params);

    /**
     * Returns the default scheme, which will be used to construct a URI when {@link
     * ManagedChannelBuilder#forTarget(String)} is given an authority string instead of a compliant
     * URI.
     *
     * @since 1.0.0
     */
    public abstract String getDefaultScheme();
  }

  /**
   * Receives address updates.
   *
   * <p>All methods are expected to return quickly.
   *
   * @since 1.0.0
   */
  @ExperimentalApi("https://github.com/grpc/grpc-java/issues/1770")
  @ThreadSafe
  public interface Listener {
    /**
     * Handles updates on resolved addresses and attributes.
     *
     * <p>Implementations will not modify the given {@code servers}.
     *
     * @param servers the resolved server addresses. An empty list will trigger {@link #onError}
     * @param attributes extra information from naming system.
     * @since 1.3.0
     */
    void onAddresses(
        List<EquivalentAddressGroup> servers, @ResolutionResultAttr Attributes attributes);

    /**
     * Handles an error from the resolver. The listener is responsible for eventually invoking
     * {@link #refresh()} to re-attempt resolution.
     *
     * @param error a non-OK status
     * @since 1.0.0
     */
    void onError(Status error);

    /**
     * 获取参数
     *
     * @author sxp
     * @since 2018/10/20
     */
    Object getArgument();

    /**
     * 设置参数
     *
     * @author sxp
     * @since 2018/10/20
     */
    void setArgument(Object argument);

    /**
     * 删除客户端与离线服务端之间的无效subchannel
     *
     * @author sxp
     * @since 2019/12/02
     */
    void removeInvalidCacheSubchannels(Set<String> removeHostPorts);
  }

  /**
   * Annotation for name resolution result attributes. It follows the annotation semantics defined
   * by {@link Attributes}.
   */
  @ExperimentalApi("https://github.com/grpc/grpc-java/issues/4972")
  @Retention(RetentionPolicy.SOURCE)
  @Documented
  public @interface ResolutionResultAttr {}
}
