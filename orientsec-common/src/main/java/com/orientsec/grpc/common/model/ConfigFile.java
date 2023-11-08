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

import com.orientsec.grpc.common.constant.GlobalConstants;
import com.orientsec.grpc.common.enums.DataType;

/**
 * 配置文件实体抽象类
 * <p>
 * 这里的配置文件指的是{@link GlobalConstants#CONFIG_FILE_PATH }
 * <p/>
 *
 * @author sxp
 * @since V1.0 2017/3/23
 */
public class ConfigFile {
  /**
   * 如果缺省值为[AUTO]，说明这个属性值是系统自动获取，不需要用户手工设置
   */
  // public static final String AUTO_VALUE = "[AUTO]";

  /**
   * 属性名
   */
  private String name;

  /**
   * 数据类型
   */
  private DataType dataType;

  /**
   * 是否必填
   */
  private boolean required;

  /**
   * 缺省值
   */
  private String defaultValue;

  /**
   * 说明
   */
  private String remark;

  public ConfigFile(String name, DataType dataType, boolean required, String defaultValue) {
    this.name = name;
    this.dataType = dataType;
    this.required = required;
    this.defaultValue = defaultValue;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DataType getDataType() {
    return dataType;
  }

  public void setDataType(DataType dataType) {
    this.dataType = dataType;
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  @Override
  public String toString() {
    return "ConfigFile{" +
            "name='" + name + '\'' +
            ", dataType=" + dataType +
            ", required=" + required +
            ", defaultValue='" + defaultValue + '\'' +
            ", remark='" + remark + '\'' +
            '}';
  }
}
