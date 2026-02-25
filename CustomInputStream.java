package bnkeditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class CustomInputStream {
    private final InputStream a;
    private final boolean b;
    private final long c;
    
    private long d;
    
    public CustomInputStream(File file, boolean littleEndian) throws FileNotFoundException {
        a = new FileInputStream(file);
        b = littleEndian;
        c = file.length();
        d = 0;
    }
    
    public byte[] readRest() throws IOException {
        if (c - d > Integer.MAX_VALUE) throw new UnsupportedOperationException("无法一次性存储这么多字节！");
        return readUntil(c);
    }
    
    public byte[] readUntil(long position) throws IOException {
        if (position < d) throw new IllegalArgumentException("该位置已被读取过！");
        else if (position - d > Integer.MAX_VALUE) throw new IllegalArgumentException("无法一次性存储这么多字节！");
        return read((int) (position - d));
    }
    
    public byte[] read(int length) throws IOException {
        byte[] e = new byte[length];
        read(e);
        return e;
    }
    
    public void read(byte[] bytes) throws IOException {
        if (c < d + bytes.length) throw new IllegalArgumentException("文件长度不足！");
        long e = d;
        while (d < e + bytes.length) d += a.read(bytes, (int) (d - e), (int) (bytes.length - (d - e)));
        if (d == c) close();
    }
    
    public String readMagic() throws IOException {
        if (c < d + 4) throw new UnsupportedOperationException("文件长度不足！");
        return readString(4);
    }
    
    public String readString(int length) throws IOException {
        if (c < d + length) throw new IllegalArgumentException("文件长度不足！");
        StringBuilder e = new StringBuilder();
        for (int f = 0; f < length; f++) e.append((char) read());
        return e.toString();
    }
    
    public long readLong() throws IOException {
        if (c < d + 8) throw new UnsupportedOperationException("文件长度不足！");
        if (b) return read() + 0x100 * read() + 0x10000 * read() + 0x1000000 * read() + 0x100000000l * read() + 0x10000000000l * read() + 0x1000000000000l * read() + 0x100000000000000l * read();
        return 0x100000000000000l * read() + 0x1000000000000l * read() + 0x10000000000l * read() + 0x100000000l * read() + 0x1000000 * read() + 0x10000 * read() + 0x100 * read() + read();
    }
    
    public int readInt() throws IOException {
        if (c < d + 4) throw new UnsupportedOperationException("文件长度不足！");
        if (b) return read() + 0x100 * read() + 0x10000 * read() + 0x1000000 * read();
        return 0x1000000 * read() + 0x10000 * read() + 0x100 * read() + read();
    }
    
    public short readShort() throws IOException {
        if (c < d + 2) throw new UnsupportedOperationException("文件长度不足！");
        if (b) return (short) (read() + 0x100 * read());
        return (short) (0x100 * read() + read());
    }
    
    public int read() throws IOException {
        int e = a.read();
        d++;
        if (d == c) close();
        return e;
    }
    
    public void skipUntil(long position) throws IOException {
        if (position < d) throw new IllegalArgumentException("该位置已被读取过！");
        skip(position - d);
    }
    
    public void skip(long amount) throws IOException {
        if (amount > c + d) throw new IllegalArgumentException("文件长度不足！");
        long e = d;
        while (d < e +amount) d += a.skip(amount - (d - e));
        if (d == c) close();
    }
    
    public void close() throws IOException {
        a.close();
    }
    
    public long getCurrentPosition() {
        return d;
    }
    
    public long getLength() {
        return c;
    }
    
    public long getRemaining() {
        return c - d;
    }
}
