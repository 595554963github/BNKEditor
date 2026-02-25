package bnkeditor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CustomOutputStream {
    private final OutputStream a;
    private final boolean b;
    
    public CustomOutputStream(File file, boolean littleEndian) throws FileNotFoundException {
        a = new FileOutputStream(file);
        b = littleEndian;
    }
    
    public void writeString(String aString) throws IOException {
        write(aString.getBytes(StandardCharsets.US_ASCII));
    }
    
    public void write(byte[] bytes) throws IOException {
        a.write(bytes);
    }
    
    public void writeLong(long aLong) throws IOException {
        if (b) {
            writeInt((int) aLong);
            writeInt((int) (aLong >> 32));
        } else {
            writeInt((int) (aLong >> 32));
            writeInt((int) aLong);
        }
    }
    
    public void writeInt(int anInt) throws IOException {
        if (b) {
            writeShort((short) anInt);
            writeShort((short) (anInt >> 16));
        } else {
            writeShort((short) (anInt >> 16));
            writeShort((short) anInt);
        }
    }
    
    public void writeShort(short aShort) throws IOException {
        if (b) {
            write(aShort);
            write(aShort >> 8);
        } else {
            write(aShort >> 8);
            write(aShort);
        }
    }
    
    public void write(int aByte) throws IOException {
        a.write(aByte);
    }
    
    public void flushAndClose() throws IOException {
        a.flush();
        a.close();
    }
}
