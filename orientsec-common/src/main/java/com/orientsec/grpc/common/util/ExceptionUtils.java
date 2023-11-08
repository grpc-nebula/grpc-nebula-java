package com.orientsec.grpc.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils {

	/**
	 * 获取异常的堆栈信息
	 * 
	 * @author Shawpin Shi
	 * @since 2016-8-26
	 */
	public static String getExceptionStackMsg(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
}
