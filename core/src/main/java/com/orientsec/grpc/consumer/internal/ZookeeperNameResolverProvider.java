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

package com.orientsec.grpc.consumer.internal;

import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.NameResolverProvider;
import io.grpc.internal.GrpcUtil;

import java.net.URI;

/**
 * A provider for {@link ZookeeperNameResolver}.
 *
 * <p>It resolves a target URI whose scheme is {@code "zookeeper"}. The (optional) authority of the target
 * URI is reserved for the address of alternative zookeeper server (not implemented yet). The path of the
 * target URI, excluding the leading slash {@code '/'}, is treated as the host name and the optional
 * port to be resolved by zookeeper. Example target URIs:
 *
 * <ul>
 *   <li>{@code "zookeeper:///foo.googleapis.com:2181"} (using default zookeeper)</li>
 *   <li>{@code "zookeeper:///foo.googleapis.com"} (without port)</li>
 * </ul>
 */
public final class ZookeeperNameResolverProvider extends NameResolverProvider {

  private static final String SCHEME = "zookeeper";

  @Override
  public ZookeeperNameResolver newNameResolver(URI targetUri, Attributes params) {
    if (SCHEME.equals(targetUri.getScheme())) {
      String targetPath = Preconditions.checkNotNull(targetUri.getPath(), "targetPath");
      Preconditions.checkArgument(targetPath.startsWith("/"),
              "the path component (%s) of the target (%s) must start with '/'", targetPath, targetUri);
      String name = targetPath.substring(1);
      return new ZookeeperNameResolver(targetUri, name, params, GrpcUtil.TIMER_SERVICE,
              GrpcUtil.SHARED_CHANNEL_EXECUTOR);
    } else {
      return null;
    }
  }

  @Override
  public String getDefaultScheme() {
    return SCHEME;
  }

  @Override
  protected boolean isAvailable() {
    return true;
  }

  @Override
  protected int priority() {
    return 3;
  }
}
