/*
 * Copyright 2017 The gRPC Authors
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

import io.grpc.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ForwardingNameResolver}.
 */
@RunWith(JUnit4.class)
public class ForwardingNameResolverTest {
  private final NameResolver delegate = mock(NameResolver.class);
  private final NameResolver forwarder = new ForwardingNameResolver(delegate) {
  };

  @Test
  public void allMethodsForwarded() throws Exception {
    ForwardingTestUtil.testMethodsForwarded(
        NameResolver.class,
        delegate,
        forwarder,
        Collections.<Method>emptyList());
  }

  @Test
  public void getServiceAuthority() {
    String auth = "example.com";
    when(delegate.getServiceAuthority()).thenReturn(auth);

    assertEquals(auth, forwarder.getServiceAuthority());
  }

  @Test
  public void start() {
    NameResolver.Listener listener = new NameResolver.Listener() {
      @Override
      public void onAddresses(List<EquivalentAddressGroup> servers, Attributes attributes) { }

      @Override
      public void onError(Status error) { }

      @Override
      public Object getArgument() {
        return null;
      }

      @Override
      public void setArgument(Object argument) {

      }
      @Override
      public void removeInvalidCacheSubchannels(Set<String> removeHostPorts) {

      }
    };

    forwarder.start(listener);
    verify(delegate).start(listener);
  }
}
