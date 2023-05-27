package com.mydb.backend.tm;

import com.mydb.common.Error;
import com.mydb.backend.paser.Parser;
import com.mydb.utils.Panic;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @ClassName : TransactionManagerImpl  //类名
 * @Description :   //描述
 * @Author : dell //作者
 * @Date: 2023/5/26  22:33
 */

public class TransactionManagerImpl implements TransactionManager {
    /**
     * XID文件头长度
     */
    static final int LEN_XID_HEADER_LENGTH = 8;

    /**
     * 每个事务的占用长度
     *
     * @return
     */
    private static final int XID_FIELD_SIZE = 1;

    /**
     * 事务的三种状态
     *
     * @return
     */
    private static final byte FIELD_TRAN_ACTIVE = 0;
    private static final byte FIELD_TRAN_COMMITTED = 1;
    private static final byte FIELD_TRAN_ABORTED = 2;

    /**
     * 超级事务，永远为committed 状态
     *
     * @return
     */
    public static final long SUPER_XID = 0;

    /**
     * XID 文件后缀
     *
     * @return
     */
    static final String XID_SUFFIX = ".xid";

    private RandomAccessFile file;
    private FileChannel fc;
    private long xidCounter;
    private Lock counterLock;

    TransactionManagerImpl(RandomAccessFile raf, FileChannel fc) {
        this.file = raf;
        this.fc = fc;
        counterLock = new ReentrantLock();
        checkXIDCounter();
    }

    /**
     * 检查XID文件是否合法
     * 读取XID_FILE_HEADER中的xidcounter，根据它计算文件的理论长度，对比实际长度
     */
    private void checkXIDCounter() {
        long fileLen = 0;
        try {
            fileLen = file.length();
        } catch (IOException e) {
            Panic.panic(Error.BAD_XID_FILE_EXCEPTION);
        }
        if (fileLen < LEN_XID_HEADER_LENGTH) {
            Panic.panic(Error.BAD_XID_FILE_EXCEPTION);
        }

        ByteBuffer buffer = ByteBuffer.allocate(LEN_XID_HEADER_LENGTH);
        try {
            //定位到 0 的位置开始操作
            fc.position(0);
            fc.read(buffer);
        } catch (IOException e) {
            Panic.panic(e);
        }
        this.xidCounter = Parser.parseLong(buffer.array());
        long end = getXidPosition(this.xidCounter + 1);
        if (end != fileLen) {
            Panic.panic(Error.BAD_XID_FILE_EXCEPTION);
        }
    }

    /**
     * 根据事务xid取得其在xid文件中对应的位置
     *
     * @param xid
     * @return
     */
    private long getXidPosition(long xid) {
        return LEN_XID_HEADER_LENGTH + (xid - 1) * XID_FIELD_SIZE;
    }


    private void updateXID(long xid, byte status) {
        long offset = getXidPosition(xid);
        byte[] tmp = new byte[XID_FIELD_SIZE];
        tmp[0] = status;
        ByteBuffer buf = ByteBuffer.wrap(tmp);
        try {
            //定位到 0 的位置开始操作
            fc.position(offset);
            fc.write(buf);
        } catch (IOException e) {
            Panic.panic(e);
        }
        try {
            fc.close();
        } catch (IOException e) {
            Panic.panic(e);
        }
    }

    /**
     * 将XID加1，并更新XID Header
     * 这里所有的文件操作，在执行后都需要立刻刷入文件中，防止在崩溃后文件丢失数据
     * filechannel 的 force() 方法 ： 强制同步缓存内容到文件中，类似于BIO 的 flush()
     * force方法的参数是一个布尔，表示是否同步文件的元数据（例如最后的修改时间等
     */
    private void incrXIDCounter() {
        xidCounter++;
        ByteBuffer buf = ByteBuffer.wrap(Parser.long2Byte(xidCounter));
        try {
            fc.position(0);
            fc.write(buf);
        } catch (IOException e) {
            Panic.panic(e);
        }
        try {
            fc.force(false);
        } catch (IOException e) {
            Panic.panic(e);
        }
    }


    /**
     * 开启一个事务， 首先设置xidcounter + 1的事务状态为committed。随后自增，并更新文件头
     *
     * @return
     */
    @Override
    public long begin() {
        counterLock.lock();
        try {
            long xid = xidCounter + 1;
            updateXID(xid, FIELD_TRAN_ABORTED);
            incrXIDCounter();
            return xid;
        } finally {
            counterLock.unlock();
        }
    }


    /**
     * 提交事务
     *
     * @param xid
     */
    @Override
    public void commit(long xid) {
        updateXID(xid, FIELD_TRAN_COMMITTED);
    }

    @Override
    public void abort(long xid) {
        updateXID(xid, FIELD_TRAN_ABORTED);
    }

    /**
     * 检查XID事务是否处理status的状态
     *
     * @param xid
     */
    private boolean checkXID(long xid, byte status) {
        long offset = getXidPosition(xid);
        ByteBuffer buf = ByteBuffer.wrap(new byte[XID_FIELD_SIZE]);
        try {
            fc.position(offset);
            fc.read(buf);
        } catch (IOException e) {
            Panic.panic(e);
        }
        return buf.array()[0] == status;
    }


    @Override
    public boolean isActive(long xid) {
        if (xid == SUPER_XID) return false;
        return checkXID(xid, FIELD_TRAN_ACTIVE);
    }

    @Override
    public boolean isCommitted(long xid) {
        if (xid == SUPER_XID) return false;
        return checkXID(xid, FIELD_TRAN_COMMITTED);
    }

    @Override
    public boolean isAborted(long xid) {
        if (xid == SUPER_XID) return false;
        return checkXID(xid, FIELD_TRAN_ABORTED);
    }

    @Override
    public void close() {
        try {
            fc.close();
            file.close();
        } catch (IOException e) {
            Panic.panic(e);
        }

    }
}
