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
package com.orientsec.grpc.common.util;

/**
 * 字节工具类
 *
 * @author sxp
 * @since 2017/9/29
 */
public class Bytes {
    private final static char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private final static int BYTE_ARRAY_MAX_LEN = Integer.MAX_VALUE >> 1;

    /**
     * 将字节数组转化成16进制的字符串
     * <p>
     * 参考资料：https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
     * <p/>
     *
     * @author sxp
     * @since 2017/9/29
     */
    public static String toHexString(byte[] bytes) {
        int len = bytes.length;
        if (len > BYTE_ARRAY_MAX_LEN) {
            throw new RuntimeException("字节数组的长度不能超过" + BYTE_ARRAY_MAX_LEN);
        }

        int newLen = len << 1;// len * 2
        char[] hexChars = new char[newLen];

        int v, index;

        for (int i = 0; i < len; i++) {
            v = bytes[i] & 0xFF; // 保留低8位，高24位置0
            index = i << 1;// index * 2
            hexChars[index] = HEX_ARRAY[v >>> 4];
            hexChars[index + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars);
    }
}
