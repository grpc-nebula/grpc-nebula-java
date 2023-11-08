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

package com.orientsec.grpc.common.model;

/**
 * 配置文件多注册中心实体抽象类
 * <p>
 * <p/>
 *
 * @author yulei
 * @since V1.0 2019/9/2
 */

public class RegistryCenter {
  private volatile String host;
  private volatile String rootPath;
  private volatile String aclUserPwd;

  /** 服务注册ip */
  private volatile String serviceIp;

  /** 服务注册端口 */
  private volatile Integer servicePort;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getRootPath() {
    return rootPath;
  }

  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  public String getAclUserPwd() {
    return aclUserPwd;
  }

  public void setAclUserPwd(String aclUserPwd) {
    this.aclUserPwd = aclUserPwd;
  }

  public String getServiceIp() {
      return serviceIp;
  }

  public void setServiceIp(String serviceIp) {
      this.serviceIp = serviceIp;
  }

  public Integer getServicePort() {
      return servicePort;
  }

  public void setServicePort(Integer servicePort) {
      this.servicePort = servicePort;
  }
}
