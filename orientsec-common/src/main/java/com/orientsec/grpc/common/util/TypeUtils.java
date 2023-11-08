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

/*
 * Copyright 1999-2017 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.orientsec.grpc.common.exception.BusinessException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;

/**
 * 类型转换工具类
 *
 * @author wenshao[szujobs@hotmail.com]
 * @since 2019-01-31 modify by sxp 删除一些不常用的方法
 */
public class TypeUtils{
    public static String castToString(Object value){
        if(value == null){
            return null;
        }
        return value.toString();
    }

    public static Byte castToByte(Object value){
        if(value == null){
            return null;
        }

        if(value instanceof BigDecimal){
            return byteValue((BigDecimal) value);
        }

        if(value instanceof Number){
            return ((Number) value).byteValue();
        }

        if(value instanceof String){
            String strVal = (String) value;
            if(strVal.length() == 0 //
                    || "null".equals(strVal) //
                    || "NULL".equals(strVal)){
                return null;
            }
            return Byte.parseByte(strVal);
        }
        throw new BusinessException("can not cast to byte, value : " + value);
    }

    public static Character castToChar(Object value){
        if(value == null){
            return null;
        }
        if(value instanceof Character){
            return (Character) value;
        }
        if(value instanceof String){
            String strVal = (String) value;
            if(strVal.length() == 0){
                return null;
            }
            if(strVal.length() != 1){
                throw new BusinessException("can not cast to char, value : " + value);
            }
            return strVal.charAt(0);
        }
        throw new BusinessException("can not cast to char, value : " + value);
    }

    public static Short castToShort(Object value){
        if(value == null){
            return null;
        }

        if(value instanceof BigDecimal){
            return shortValue((BigDecimal) value);
        }

        if(value instanceof Number){
            return ((Number) value).shortValue();
        }

        if(value instanceof String){
            String strVal = (String) value;
            if(strVal.length() == 0 //
                    || "null".equals(strVal) //
                    || "NULL".equals(strVal)){
                return null;
            }
            return Short.parseShort(strVal);
        }

        throw new BusinessException("can not cast to short, value : " + value);
    }

    public static BigDecimal castToBigDecimal(Object value){
        if(value == null){
            return null;
        }
        if(value instanceof BigDecimal){
            return (BigDecimal) value;
        }
        if(value instanceof BigInteger){
            return new BigDecimal((BigInteger) value);
        }
        String strVal = value.toString();
        if(strVal.length() == 0){
            return null;
        }
        if(value instanceof Map && ((Map) value).size() == 0){
            return null;
        }
        return new BigDecimal(strVal);
    }

    public static BigInteger castToBigInteger(Object value){
        if(value == null){
            return null;
        }
        if(value instanceof BigInteger){
            return (BigInteger) value;
        }
        if(value instanceof Float || value instanceof Double){
            return BigInteger.valueOf(((Number) value).longValue());
        }
        if(value instanceof BigDecimal){
            BigDecimal decimal = (BigDecimal) value;
            int scale = decimal.scale();
            if (scale > -1000 && scale < 1000) {
                return ((BigDecimal) value).toBigInteger();
            }
        }
        String strVal = value.toString();
        if(strVal.length() == 0 //
                || "null".equals(strVal) //
                || "NULL".equals(strVal)){
            return null;
        }
        return new BigInteger(strVal);
    }

    public static Float castToFloat(Object value){
        if(value == null){
            return null;
        }
        if(value instanceof Number){
            return ((Number) value).floatValue();
        }
        if(value instanceof String){
            String strVal = value.toString();
            if(strVal.length() == 0 //
                    || "null".equals(strVal) //
                    || "NULL".equals(strVal)){
                return null;
            }
            if(strVal.indexOf(',') != 0){
                strVal = strVal.replaceAll(",", "");
            }
            return Float.parseFloat(strVal);
        }
        throw new BusinessException("can not cast to float, value : " + value);
    }

    public static Double castToDouble(Object value){
        if(value == null){
            return null;
        }
        if(value instanceof Number){
            return ((Number) value).doubleValue();
        }
        if(value instanceof String){
            String strVal = value.toString();
            if(strVal.length() == 0 //
                    || "null".equals(strVal) //
                    || "NULL".equals(strVal)){
                return null;
            }
            if(strVal.indexOf(',') != 0){
                strVal = strVal.replaceAll(",", "");
            }
            return Double.parseDouble(strVal);
        }
        throw new BusinessException("can not cast to double, value : " + value);
    }


    public static java.sql.Date castToSqlDate(Object value){
        if(value == null){
            return null;
        }
        if(value instanceof java.sql.Date){
            return (java.sql.Date) value;
        }
        if(value instanceof java.util.Date){
            return new java.sql.Date(((java.util.Date) value).getTime());
        }
        if(value instanceof Calendar){
            return new java.sql.Date(((Calendar) value).getTimeInMillis());
        }

        long longValue = 0;
        if(value instanceof BigDecimal){
            longValue = longValue((BigDecimal) value);
        } else if(value instanceof Number){
            longValue = ((Number) value).longValue();
        }

        if(value instanceof String){
            String strVal = (String) value;
            if(strVal.length() == 0 //
                    || "null".equals(strVal) //
                    || "NULL".equals(strVal)){
                return null;
            }
            if(isNumber(strVal)){
                longValue = Long.parseLong(strVal);
            }
        }
        if(longValue <= 0){
            throw new BusinessException("can not cast to Date, value : " + value);
        }
        return new java.sql.Date(longValue);
    }

    public static long longExtractValue(Number number) {
        if (number instanceof BigDecimal) {
            return ((BigDecimal) number).longValueExact();
        }

        return number.longValue();
    }

    public static java.sql.Time castToSqlTime(Object value){
        if(value == null){
            return null;
        }
        if(value instanceof java.sql.Time){
            return (java.sql.Time) value;
        }
        if(value instanceof java.util.Date){
            return new java.sql.Time(((java.util.Date) value).getTime());
        }
        if(value instanceof Calendar){
            return new java.sql.Time(((Calendar) value).getTimeInMillis());
        }

        long longValue = 0;
        if(value instanceof BigDecimal){
            longValue = longValue((BigDecimal) value);
        } else if(value instanceof Number){
            longValue = ((Number) value).longValue();
        }

        if(value instanceof String){
            String strVal = (String) value;
            if(strVal.length() == 0 //
                    || "null".equalsIgnoreCase(strVal)){
                return null;
            }
            if(isNumber(strVal)){
                longValue = Long.parseLong(strVal);
            }
        }
        if(longValue <= 0){
            // TODO 忽略 1970-01-01 之前的时间处理？
            throw new BusinessException("can not cast to Date, value : " + value);
        }
        return new java.sql.Time(longValue);
    }

    public static java.sql.Timestamp castToTimestamp(Object value){
        if(value == null){
            return null;
        }
        if(value instanceof Calendar){
            return new java.sql.Timestamp(((Calendar) value).getTimeInMillis());
        }
        if(value instanceof java.sql.Timestamp){
            return (java.sql.Timestamp) value;
        }
        if(value instanceof java.util.Date){
            return new java.sql.Timestamp(((java.util.Date) value).getTime());
        }
        long longValue = 0;
        if(value instanceof BigDecimal){
            longValue = longValue((BigDecimal) value);
        } else if(value instanceof Number){
            longValue = ((Number) value).longValue();
        }
        if(value instanceof String){
            String strVal = (String) value;
            if(strVal.length() == 0 //
                    || "null".equals(strVal) //
                    || "NULL".equals(strVal)){
                return null;
            }
            if(strVal.endsWith(".000000000")){
                strVal = strVal.substring(0, strVal.length() - 10);
            } else if(strVal.endsWith(".000000")){
                strVal = strVal.substring(0, strVal.length() - 7);
            }
            if(isNumber(strVal)){
                longValue = Long.parseLong(strVal);
            }
        }
        if(longValue <= 0){
            // TODO 忽略 1970-01-01 之前的时间处理？
            throw new BusinessException("can not cast to Timestamp, value : " + value);
        }
        return new java.sql.Timestamp(longValue);
    }

    public static boolean isNumber(String str){
        for(int i = 0; i < str.length(); ++i){
            char ch = str.charAt(i);
            if(ch == '+' || ch == '-'){
                if(i != 0){
                    return false;
                }
            } else if(ch < '0' || ch > '9'){
                return false;
            }
        }
        return true;
    }

    public static Long castToLong(Object value){
        if(value == null){
            return null;
        }

        if(value instanceof BigDecimal){
            return longValue((BigDecimal) value);
        }

        if(value instanceof Number){
            return ((Number) value).longValue();
        }

        if(value instanceof String){
            String strVal = (String) value;
            if(strVal.length() == 0 //
                    || "null".equals(strVal) //
                    || "NULL".equals(strVal)){
                return null;
            }
            if(strVal.indexOf(',') != 0){
                strVal = strVal.replaceAll(",", "");
            }
            try{
                return Long.parseLong(strVal);
            } catch(NumberFormatException ex){
                //
            }
        }

        if(value instanceof Map){
            Map map = (Map) value;
            if(map.size() == 2
                    && map.containsKey("andIncrement")
                    && map.containsKey("andDecrement")){
                Iterator iter = map.values().iterator();
                iter.next();
                Object value2 = iter.next();
                return castToLong(value2);
            }
        }

        throw new BusinessException("can not cast to long, value : " + value);
    }

    public static byte byteValue(BigDecimal decimal) {
        if (decimal == null) {
            return 0;
        }

        int scale = decimal.scale();
        if (scale >= -100 && scale <= 100) {
            return decimal.byteValue();
        }

        return decimal.byteValueExact();
    }

    public static short shortValue(BigDecimal decimal) {
        if (decimal == null) {
            return 0;
        }

        int scale = decimal.scale();
        if (scale >= -100 && scale <= 100) {
            return decimal.shortValue();
        }

        return decimal.shortValueExact();
    }

    public static int intValue(BigDecimal decimal) {
        if (decimal == null) {
            return 0;
        }

        int scale = decimal.scale();
        if (scale >= -100 && scale <= 100) {
            return decimal.intValue();
        }

        return decimal.intValueExact();
    }

    public static long longValue(BigDecimal decimal) {
        if (decimal == null) {
            return 0;
        }

        int scale = decimal.scale();
        if (scale >= -100 && scale <= 100) {
            return decimal.longValue();
        }

        return decimal.longValueExact();
    }

    public static Integer castToInt(Object value){
        if(value == null){
            return null;
        }

        if(value instanceof Integer){
            return (Integer) value;
        }

        if(value instanceof BigDecimal){
            return intValue((BigDecimal) value);
        }

        if(value instanceof Number){
            return ((Number) value).intValue();
        }

        if(value instanceof String){
            String strVal = (String) value;
            if(strVal.length() == 0 //
                    || "null".equals(strVal) //
                    || "NULL".equals(strVal)){
                return null;
            }
            if(strVal.indexOf(',') != 0){
                strVal = strVal.replaceAll(",", "");
            }
            return Integer.parseInt(strVal);
        }

        if(value instanceof Boolean){
            return ((Boolean) value).booleanValue() ? 1 : 0;
        }
        if(value instanceof Map){
            Map map = (Map) value;
            if(map.size() == 2
                    && map.containsKey("andIncrement")
                    && map.containsKey("andDecrement")){
                Iterator iter = map.values().iterator();
                iter.next();
                Object value2 = iter.next();
                return castToInt(value2);
            }
        }
        throw new BusinessException("can not cast to int, value : " + value);
    }

    /**
     *
     * @since 2019-01-31 modify by sxp 修改判断逻辑
     */
    public static Boolean castToBoolean(Object value){
        if(value == null){
            return null;
        }
        if(value instanceof Boolean){
            return (Boolean) value;
        }
        if(value instanceof String){
            String s = (String) value;
            return ((s != null) && s.equalsIgnoreCase("true"));
        }
        throw new BusinessException("can not cast to boolean, value : " + value);
    }

    public static boolean castToBooleanValue(Object value){
        Boolean result = castToBoolean(value);
        if (result == null) {
            return false;
        } else {
            return result.booleanValue();
        }
    }


}
