package com.mydb.backend.tm;

import com.mydb.common.Error;
import com.mydb.utils.Panic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @ClassName : TransactionManager  //类名
 * @Description :   //描述
 * @Author : dell //作者
 * @Date: 2023/5/26  22:29
 */

public interface TransactionManager {
    /**
     * 开启一个新事务
     *
     * @return
     */
    long begin();

    /**
     * 提交一个事务
     *
     * @param xid
     */
    void commit(long xid);

    /**
     * 取消一个事务
     *
     * @param xid
     */
    void abort(long xid);

    /**
     * 查询一个事务的状态是否正在进行
     *
     * @param xid
     * @return
     */
    boolean isActive(long xid);

    /**
     * 查询一个事务的状态是否已经提交
     *
     * @param xid
     * @return
     */
    boolean isCommitted(long xid);

    /**
     * 查询一个事务的状态是否已经取消
     *
     * @param xid
     * @return
     */
    boolean isAborted(long xid);

    /**
     * 关闭TM
     */
    void close();


    /**
     * 创建一个 xid 文件并创建 TM
     *
     * @param path
     * @return
     */
    public static TransactionManagerImpl create(String path) {
        File f = new File(path + TransactionManagerImpl.XID_SUFFIX);
        try {
            if (!f.createNewFile()) {
                Panic.panic(Error.FILE_EXISTS_EXCEPTION);
            }
        } catch (Exception e) {
            Panic.panic(e);
        }
        if (!f.canRead() || !f.canWrite()) {
            Panic.panic(Error.FILE_CANNOTRW_EXCEPTION);
        }

        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            // RandomAccessFile rw 打开以便读取和写入
            raf = new RandomAccessFile(f, "rw");
            fc = raf.getChannel();
        } catch (FileNotFoundException e) {
            Panic.panic(e);
        }
        // 写空XID文件头
        ByteBuffer buf = ByteBuffer.wrap(new byte[TransactionManagerImpl.LEN_XID_HEADER_LENGTH]);
        try {
            fc.position(0);
            fc.write(buf);
        } catch (IOException e) {
            Panic.panic(e);
        }
        return new TransactionManagerImpl(raf, fc);
    }


    /**
     * 从一个已有的 xid 文件来创建 TM
     */
    public static TransactionManagerImpl open(String path) {
        File f = new File(path + TransactionManagerImpl.XID_SUFFIX);
        if (!f.exists()) {
            Panic.panic(Error.FILE_NOT_EXISTS_EXCEPTION);
        }
        if (!f.canRead() || f.canWrite()) {
            Panic.panic(Error.FILE_CANNOTRW_EXCEPTION);
        }

        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "rw");
            fc = raf.getChannel();
        } catch (FileNotFoundException e) {
            Panic.panic(e);
        }

        return new TransactionManagerImpl(raf, fc);
    }
}
