package bnkeditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

public class EditorWindow extends JFrame {
    private static final String STATUS_WORKING = "处理中...", STATUS_DONE = "完成。", STATUS_FAILED = "失败。";
    private static final FileFilter BNK_FILTER = new FileFilter() {
        @Override public boolean accept(File f) {
            String s = f.getAbsolutePath();
            return f.isDirectory() || ".bnk".equals(s.substring(s.length() - 4).toLowerCase());
        }

        @Override public String getDescription() {
            return "Audiokinetic Wwise声音库 (*.bnk)";
        }
    }, WEM_FILTER = new FileFilter() {
        @Override public boolean accept(File f) {
            String s = f.getAbsolutePath();
            return f.isDirectory() || ".wem".equals(s.substring(s.length() - 4).toLowerCase());
        }

        @Override public String getDescription() {
            return "Audiokinetic Wwise编码媒体 (*.wem)";
        }
    };
    
    private final JCheckBox littleEndian;
    private final JButton saveAllWEMsButton, saveBNKButton;
    private final JLabel bnkName, status;
    private final JPanel list;
    private final JFileChooser openBNK, saveWEM, openWEM, saveAllWEMs, saveBNK;
    
    private BNKEditor editor;
    private int[] ids;
    private JLabel[] replacementNames;
    
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override public void run() {
                EditorWindow window = new EditorWindow();
            }
        });
    }
    
    public EditorWindow () {
        list = new JPanel(new GridLayout(0, 5));
        littleEndian = new JCheckBox("小端序", true);
        littleEndian.setToolTipText("不确定Wwise是否使用大端序。如果持续遇到错误，请尝试取消勾选此选项并重新打开BNK文件。");
        bnkName = new JLabel();
        saveAllWEMsButton = new JButton("导出所有WEM到...");
        saveAllWEMsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAllWEMsButtonPressed();
            }
        });
        saveAllWEMsButton.setEnabled(false);
        saveBNKButton = new JButton("保存BNK到...");
        saveBNKButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent ae) {
                saveBNKButtonPressed();
            }
        });
        saveBNKButton.setEnabled(false);
        status = new JLabel();
        openBNK = new JFileChooser();
        openBNK.setFileFilter(BNK_FILTER);
        openBNK.setDialogTitle("选择要编辑的BNK文件");
        saveWEM = new JFileChooser();
        saveWEM.setFileFilter(WEM_FILTER);
        saveWEM.setDialogTitle("选择要保存所选WEM的位置");
        openWEM = new JFileChooser();
        openWEM.setFileFilter(WEM_FILTER);
        openWEM.setDialogTitle("选择要替换的WEM文件");
        saveAllWEMs = new JFileChooser();
        saveAllWEMs.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        saveAllWEMs.setDialogTitle("选择要保存所有WEM的文件夹");
        saveBNK = new JFileChooser();
        saveBNK.setFileFilter(BNK_FILTER);
        saveBNK.setDialogTitle("选择要保存BNK文件的位置");
        initComponents();
    }
    
    private void initComponents() {
        JPanel jp = new JPanel();
        jp.add(littleEndian);
        JButton jb = new JButton("打开BNK文件...");
        jb.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent ae) {
                openBNKButtonPressed();
            }
        });
        jp.add(jb);
        jp.add(bnkName);
        jp.add(saveAllWEMsButton);
        jp.add(saveBNKButton);
        jp.add(status);
        add(jp, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(new Dimension((int) (d.width * 0.75), (int) (d.height * 0.75)));
        setTitle("BNK编辑器 v1.0");
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void openBNKButtonPressed() {
        try {
            if (openBNK.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            status.setText(STATUS_WORKING);
            editor = new BNKEditor(openBNK.getSelectedFile(), littleEndian.isSelected());
            bnkName.setText(openBNK.getSelectedFile().getName());
            saveAllWEMsButton.setEnabled(true);
            saveBNKButton.setEnabled(true);
            ids = editor.getIDs();
            replacementNames = new JLabel[ids.length];
            list.removeAll();
            for (int i = 0; i < ids.length; i++) {
                final int id = i;
                list.add(new JLabel(Integer.toUnsignedString(ids[id])));
                JButton jb = new JButton("导出WEM到...");
                jb.addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent ae) {
                        saveWEMButtonPressed(id);
                    }
                });
                list.add(jb);
                jb = new JButton("替换WEM为...");
                jb.addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent ae) {
                        replaceWEMButtonPressed(id);
                    }
                });
                list.add(jb);
                replacementNames[id] = new JLabel();
                list.add(replacementNames[id]);
                jb = new JButton("取消");
                jb.addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent ae) {
                        cancelReplacementButtonPressed(id);
                    }
                });
                list.add(jb);
            }
            revalidate();
            status.setText(STATUS_DONE);
        } catch (IOException ioe) {
            editor = null;
            saveAllWEMsButton.setEnabled(false);
            saveBNKButton.setEnabled(false);
            ids = null;
            replacementNames = null;
            list.removeAll();
            revalidate();
            status.setText(STATUS_FAILED);
            JOptionPane.showMessageDialog(this, "读取文件时发生以下异常: " + ioe.getMessage(), "IO异常", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException iae) {
            editor = null;
            saveAllWEMsButton.setEnabled(false);
            saveBNKButton.setEnabled(false);
            ids = null;
            replacementNames = null;
            list.removeAll();
            revalidate();
            status.setText(STATUS_FAILED);
            JOptionPane.showMessageDialog(this, "你打开的文件存在以下问题: " + iae.getMessage(), "非法参数异常", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            editor = null;
            saveAllWEMsButton.setEnabled(false);
            saveBNKButton.setEnabled(false);
            ids = null;
            replacementNames = null;
            list.removeAll();
            revalidate();
            status.setText(STATUS_FAILED);
            JOptionPane.showMessageDialog(this, "发生以下错误: " + e.getMessage() + "\n原因未知", e.getClass().getName(), JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void saveWEMButtonPressed(int id) {
        try {
            if (saveWEM.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            status.setText(STATUS_WORKING);
            editor.writeWEM(id, false, saveWEM.getSelectedFile());
            status.setText(STATUS_DONE);
        } catch (IOException ioe) {
            status.setText(STATUS_FAILED);
            JOptionPane.showMessageDialog(this, "读取或写入文件时发生以下异常: " + ioe.getMessage(), "IO异常", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void replaceWEMButtonPressed(int id) {
        try {
            if (openWEM.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            status.setText(STATUS_WORKING);
            File replacement = openWEM.getSelectedFile();
            editor.replace(id, false, replacement);
            replacementNames[id].setText(replacement.getName());
            status.setText(STATUS_DONE);
        } catch (IllegalArgumentException iae) {
            status.setText(STATUS_FAILED);
            JOptionPane.showMessageDialog(this, "你打开的文件存在以下问题: " + iae.getMessage(), "非法参数异常", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void cancelReplacementButtonPressed(int id) {
        status.setText(STATUS_WORKING);
        editor.cancelReplacement(id, false);
        replacementNames[id].setText(null);
        status.setText(STATUS_DONE);
    }
    
    private void saveAllWEMsButtonPressed() {
        try {
            if (saveAllWEMs.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            status.setText(STATUS_WORKING);
            File directory = saveAllWEMs.getSelectedFile();
            for (int i = 0; i < ids.length; i++) {
                File file = new File(directory, (i + 1) + "_" + ids[i] + ".wem");
                editor.writeWEM(i, false, file);
            }
            status.setText(STATUS_DONE);
        } catch (IOException ioe) {
            status.setText(STATUS_FAILED);
            JOptionPane.showMessageDialog(this, "读取或写入文件时发生以下异常: " + ioe.getMessage(), "IO异常", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveBNKButtonPressed() {
        try {
            if (saveBNK.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            status.setText(STATUS_WORKING);
            editor.writeBNK(saveBNK.getSelectedFile(), littleEndian.isSelected());
            status.setText(STATUS_DONE);
        } catch (IOException ioe) {
            status.setText(STATUS_FAILED);
            JOptionPane.showMessageDialog(this, "读取或写入文件时发生以下异常: " + ioe.getMessage(), "IO异常", JOptionPane.ERROR_MESSAGE);
        }
    }
}
