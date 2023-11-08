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
package com.orientsec.grpc.provider.core;

import com.orientsec.grpc.common.exception.BusinessException;

import java.util.List;
import java.util.Map;

/**
 * 服务注册
 * <p>
 * 1.服务提供者启动服务时，自动注册该服务 <br>
 * 2.服务提供者停止服务时，自动注销服务
 * </p>
 *
 * @author sxp
 * @since V1.0 2017/3/20
 */
public interface ProviderServiceRegistry {
  /**
   * 注册服务
   *
   * @param servicesParams 服务的属性
   * @author sxp
   * @since V1.0 2017/3/20
   */
  void register(List<Map<String, Object>> servicesParams) throws BusinessException;

  /**
   * 注销服务
   *
   * @param servicesParams 服务的属性
   * @author sxp
   * @since V1.0 2017/3/20
   */
  void unRegister(List<Map<String, Object>> servicesParams) throws BusinessException;
}
