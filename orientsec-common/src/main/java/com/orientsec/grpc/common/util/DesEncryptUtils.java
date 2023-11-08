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


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * DES加密工具类
 *
 * @author sxp
 * @since 2019/2/12
 */
public class DesEncryptUtils {
  private static final String ALGORITHM = "PBEWithMD5AndDES";

  private static final char[] PRIVATE_KEY_PWD = "Orientsec#62968980#Bocloud".toCharArray();
  private static final Key PRIVATE_KEY = getPBEKey();

  private static final byte[] SALT_BYTES = "20122018".getBytes(StandardCharsets.UTF_8);
  private static final int ITERATION_COUNT = 1000;

  /**
   * 根据PBE密码生成一把密钥
   */
  private static Key getPBEKey() {
    SecretKeyFactory keyFactory;
    SecretKey secretKey;

    try {
      keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
      PBEKeySpec keySpec = new PBEKeySpec(PRIVATE_KEY_PWD);
      secretKey = keyFactory.generateSecret(keySpec);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    return secretKey;
  }

  /**
   * 加密
   *
   * @param plaintext 待加密的明文字符串
   * @return 加密后的密文字符串
   */
  public static String encrypt(String plaintext) {
    byte[] encipheredData = null;
    PBEParameterSpec parameterSpec = new PBEParameterSpec(SALT_BYTES, ITERATION_COUNT);
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);

      cipher.init(Cipher.ENCRYPT_MODE, PRIVATE_KEY, parameterSpec);

      encipheredData = cipher.doFinal(plaintext.getBytes());
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return bytesToHexString(encipheredData);
  }

  /**
   * 解密密文字符串
   *
   * @param ciphertext 待解密的密文字符串
   * @return 解密后的明文字符串
   */
  public static String decrypt(String ciphertext) {
    byte[] passDec = null;

    PBEParameterSpec parameterSpec = new PBEParameterSpec(SALT_BYTES, ITERATION_COUNT);
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);

      cipher.init(Cipher.DECRYPT_MODE, PRIVATE_KEY, parameterSpec);

      passDec = cipher.doFinal(hexStringToBytes(ciphertext));
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return new String(passDec);
  }

  /**
   * 将字节数组转换为十六进制字符串
   */
  public static String bytesToHexString(byte[] bytes) {
    return Bytes.toHexString(bytes);
  }

  /**
   * 将十六进制字符串转换为字节数组
   */
  public static byte[] hexStringToBytes(String hexString) {
    if (StringUtils.isEmpty(hexString)) {
      return null;
    }
    hexString = hexString.toUpperCase();
    int length = hexString.length() / 2;
    char[] hexChars = hexString.toCharArray();
    byte[] d = new byte[length];
    for (int i = 0; i < length; i++) {
      int pos = i * 2;
      d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
    }
    return d;
  }

  private static byte charToByte(char c) {
    return (byte) "0123456789ABCDEF".indexOf(c);
  }

  /**
   * Test
   *
   * @author sxp
   * @since 2019/2/12
   */
  public static void main(String[] args) {
    String plaintext = "bocloud@123";

    System.out.println("加密前:" + plaintext);

    String ciphertext = DesEncryptUtils.encrypt(plaintext);
    System.out.println("加密后:" + ciphertext);

    String decryptedText = DesEncryptUtils.decrypt(ciphertext);
    System.out.println("解密后:" + decryptedText);

  }
}
