package bnkeditor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class BNKEditor {
    private final CustomInputStream input;
    private final byte[] bkhd;
    private final int numWEMs;
    private final byte[][] bufferedWEMs;
    private final int[] ids, offsets, originalLengths, replacedLengths;
    private final File[] replacements;
    private final int offsetAbsolute, dataLength;
    
    private byte[] rest;
    
    public BNKEditor(File bnk, boolean littleEndian) throws IOException {
        if (bnk.length() > Integer.MAX_VALUE) throw new IllegalArgumentException("文件过大！");
        input = new CustomInputStream(bnk, littleEndian);
        
        if (!"BKHD".equals(input.readMagic())) throw new IllegalArgumentException("文件没有BKHD段！");
        int bkhdLength = input.readInt();
        bkhd = input.read(bkhdLength);
        
        if (!"DIDX".equals(input.readMagic())) throw new IllegalArgumentException("文件没有DIDX段！");
        int didxLength = input.readInt();
        if (didxLength % 12 != 0) throw new IllegalArgumentException("文件的DIDX段已损坏！(长度为 " + didxLength + "，无法被12整除)");
        
        numWEMs = didxLength / 12;
        bufferedWEMs = new byte[numWEMs][];
        ids = new int[numWEMs];
        offsets = new int[numWEMs];
        originalLengths = new int[numWEMs];
        replacedLengths = new int[numWEMs];
        replacements = new File[numWEMs];
        for (int i = 0; i < numWEMs; i++) {
            int id = input.readInt(), offset = input.readInt(), length_ = input.readInt();
            if (i > 0 && offset < offsets[i - 1]) throw new IllegalArgumentException("文件的DIDX段已损坏！(第 " + (i + 1) + " 个WEM位于偏移量 " + offset + "，而第 " + i + " 个WEM位于偏移量 " + offsets[i - 1] + ")");
            ids[i] = id;
            offsets[i] = offset;
            originalLengths[i] = length_;
            replacedLengths[i] = length_;
        }
        
        if (!"DATA".equals(input.readMagic())) throw new IllegalArgumentException("文件没有DATA段！");
        dataLength = input.readInt();
        int calc = 0;
        for (int wem = 0; wem < numWEMs; wem++) {
            calc += originalLengths[wem];
        }
        if (dataLength < calc) throw new IllegalArgumentException("文件的DATA段已损坏！(计算长度: " + calc + "，实际长度: " + dataLength + ")");
        offsetAbsolute = (int) input.getCurrentPosition();
    }
    
    public int[] getIDs() {
        return ids;
    }
    
    public void writeWEM(int index, boolean isID, File wem) throws IOException {
        int position = index;
        if (isID) {
            for (int i = 0; i < numWEMs; i++) {
                if (ids[i] == index) {
                    position = i;
                    break;
                }
            }
        }
        
        for (int i = 0; i <= position; i++) {
            if (input.getCurrentPosition() <= offsets[i] + offsetAbsolute) {
                input.skipUntil(offsets[i] + offsetAbsolute);
                bufferedWEMs[i] = input.read(originalLengths[i]);
            }
        }
        
        wem.createNewFile();
        CustomOutputStream output = new CustomOutputStream(wem, true);
        output.write(bufferedWEMs[position]);
        output.flushAndClose();
    }
    
    public void replace(int index, boolean isID, File replacement) {
        if (replacement.length() > Integer.MAX_VALUE) throw new IllegalArgumentException("WEM文件过大！");
        
        int position = index;
        if (isID) {
            for (int i = 0; i < numWEMs; i++) {
                if (ids[i] == index) {
                    position = i;
                    break;
                }
            }
        }
        
        replacements[position] = replacement;
        replacedLengths[position] = (int) replacement.length();
    }
    
    public void cancelReplacement(int index, boolean isID) {
        int position = index;
        if (isID) {
            for (int i = 0; i < numWEMs; i++) {
                if (ids[i] == index) {
                    position = i;
                    break;
                }
            }
        }
        
        replacements[position] = null;
        replacedLengths[position] = originalLengths[position];
    }
    
    public File[] getReplacements() {
        return replacements;
    }
    
    public void writeBNK(File bnk, boolean littleEndian) throws IOException {
        bnk.createNewFile();
        CustomOutputStream output = new CustomOutputStream(bnk, littleEndian);
        
        output.writeString("BKHD");
        output.writeInt(bkhd.length);
        output.write(bkhd);
        
        output.writeString("DIDX");
        output.writeInt(numWEMs * 12);
        
        output.writeInt(ids[0]);
        output.writeInt(0);
        output.writeInt(replacedLengths[0]);
        int currentAddress = replacedLengths[0];
        for (int i = 1; i < numWEMs; i++) {
            output.writeInt(ids[i]);
            output.writeInt(currentAddress);
            output.writeInt(replacedLengths[i]);
            currentAddress += replacedLengths[i];
        }
        
        output.writeString("DATA");
        int calc = 0;
        for (int i = 0; i < numWEMs; i++) {
            calc += replacedLengths[i];
        }
        output.writeInt(calc);
        
        for (int i = 0; i < numWEMs; i++) {
            if (replacements[i] != null) {
                CustomInputStream replacement = new CustomInputStream(replacements[i], littleEndian);
                output.write(replacement.readRest());
                continue;
            }
            for (int j = 0; j <= i; j++) {
                if (input.getCurrentPosition() <= offsets[j] + offsetAbsolute) {
                    input.skipUntil(offsets[j] + offsetAbsolute);
                    bufferedWEMs[j] = input.read(originalLengths[j]);
                }
            }
            output.write(bufferedWEMs[i]);
        }
        
        if (input.getRemaining() > 0) rest = input.readRest();
        output.write(rest);
        output.flushAndClose();
    }
}
