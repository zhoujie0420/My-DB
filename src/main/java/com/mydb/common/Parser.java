package com.mydb.common;

import java.nio.ByteBuffer;

/**
 * @ClassName : Parser  //类名
 * @Description :   //描述
 * @Author : dell //作者
 * @Date: 2023/5/26  23:24
 */

public class Parser {
    public static long parseLong(byte[] buf){
        ByteBuffer buffer = ByteBuffer.wrap(buf, 0, 0);
        return buffer.getLong();
    }

    public static byte[] long2Byte(long value){
        return ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(value).array();
    }
}
