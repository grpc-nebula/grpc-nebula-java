package com.orientsec.grpc.common.util;

import org.junit.Test;

/**
 * Unit Test for ExceptionUtils
 *
 * @author sxp
 * @since 2019/7/9
 */
public class ExceptionUtilsTest {
  @Test
  public void getExceptionStackMsgUsage() {
    String text = null;
    int a = 10;
    int b = 0;

    try {
      if (a % b == 0) {
        text = "sxp";
      }
      if (text.equals("sxp")) {
        System.out.println("equals");
      }
    } catch (Exception e) {
      System.out.print("ExceptionStackMsg:");
      System.out.println(ExceptionUtils.getExceptionStackMsg(e));
    }
  }
}