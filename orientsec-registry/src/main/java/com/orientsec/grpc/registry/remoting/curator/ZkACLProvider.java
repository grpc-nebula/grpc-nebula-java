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

import org.apache.curator.framework.api.ACLProvider;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


/**
 * zookeeper访问控制提供者
 *
 * @author sxp
 * @since 2019/2/12
 */
public class ZkACLProvider implements ACLProvider {
  private static final String SCHEME = "digest";
  private final String userPassword;
  private List<ACL> acl;

  public ZkACLProvider(String userPassword) {
      this.userPassword = userPassword;
  }

  @Override
  public List<ACL> getDefaultAcl() {
    if (acl == null) {
      ArrayList<ACL> acl = ZooDefs.Ids.CREATOR_ALL_ACL;
      acl.clear();

      String auth;
      try {
        auth = DigestAuthenticationProvider.generateDigest(userPassword);
      } catch (NoSuchAlgorithmException e) {
        auth = "";
      }

      acl.add(new ACL(ZooDefs.Perms.ALL, new Id(SCHEME, auth)));
      this.acl = acl;
    }
    return acl;
  }

  @Override
  public List<ACL> getAclForPath(String path) {
    return acl;
  }

  public static String getScheme() {
    return SCHEME;
  }

}
