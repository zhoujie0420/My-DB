package com.mydb.backend.common;

/**
 * @ClassName : SubArray  //类名
 * @Description :   //描述
 * @Author : dell //作者
 * @Date: 2023/5/27  16:27
 */


public class SubArray {
    public byte[] raw;
    public int start;
    public int end;

    public SubArray(byte[] raw, int start, int end) {
        this.raw = raw;
        this.start = start;
        this.end = end;
    }
}