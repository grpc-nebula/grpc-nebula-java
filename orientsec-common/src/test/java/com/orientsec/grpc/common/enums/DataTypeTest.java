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
package com.orientsec.grpc.common.enums;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for DataType
 *
 * @author sxp
 * @since 2018/11/28
 */
public class DataTypeTest {

    @Test
    public void usage() throws Exception {
        Assert.assertEquals("STRING", DataType.STRING.name());
        Assert.assertEquals("String", DataType.STRING.getSimpleName());

        DataType type = DataType.BOOLEAN;
        Assert.assertEquals("boolean", type.getSimpleName());

        String randomText = "the name you like";

        type.setSimpleName(randomText);
        Assert.assertEquals(randomText, type.getSimpleName());
    }

}
