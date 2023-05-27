package com.mydb.utils;

/**
 * @ClassName : Panic  //类名
 * @Description :   // 程序强制退出
 * @Author : dell //作者
 * @Date: 2023/5/26  23:29
 */

public class Panic {
    public static void panic(Exception err){
        err.printStackTrace();
        System.exit(1);
    }
}
