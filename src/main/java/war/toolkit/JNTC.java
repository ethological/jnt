package war.toolkit;

import javax.swing.*;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import war.jnt.crypto.Crypto;

public class JNTC extends JFrame {

    private final JTree tree;
    private final RSyntaxTextArea codeArea;
    private final JTable metadataTable;
    private final DefaultTableModel metaModel;
    private final JList<String> errorList;
    private final DefaultListModel<String> errorModel;

    private ZipEntry currentEntry; 
    private final JCheckBoxMenuItem wrapToggle = new JCheckBoxMenuItem("Wrap Lines", true);

    private ZipFile currentZip;

    public JNTC() {
        super("jntc | welcome, " + System.getProperty("user.name") + "!");
        Font uiFont;
        try {
            uiFont = new Font("Inter", Font.PLAIN, 13);
            if (uiFont == null || !uiFont.getFamily().equals("Inter")) {
                uiFont = new Font(Font.MONOSPACED, Font.PLAIN, 13);
            }
        } catch (Exception ex) {
            uiFont = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        }

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        tree = new JTree();
        tree.setFont(new Font("Inter", Font.PLAIN, 13));
        JScrollPane treeScroll = new JScrollPane(tree);

        codeArea = new RSyntaxTextArea();
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setEditable(false);
        codeArea.setLineWrap(true);
        codeArea.setWrapStyleWord(true);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        RTextScrollPane codeScroll = new RTextScrollPane(codeArea);

        metaModel = new DefaultTableModel(new Object[]{"Key", "Value"}, 0);
        metadataTable = new JTable(metaModel);
        JScrollPane metaScroll = new JScrollPane(metadataTable);

        errorModel = new DefaultListModel<>();
        errorList = new JList<>(errorModel);

        errorList.setCellRenderer(new DefaultListCellRenderer() {
            private final Icon errorIcon = UIManager.getIcon("OptionPane.errorIcon");

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String errorText = value.toString();

                label.setText("<html><div style='padding:4px;'>" + errorText + "</div></html>");
                label.setFont(new Font("Inter", Font.PLAIN, 13));
                label.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
                label.setIcon(errorIcon);

                label.setHorizontalTextPosition(SwingConstants.RIGHT);
                label.setVerticalTextPosition(SwingConstants.CENTER);
                label.setIconTextGap(10);

                if (isSelected) {
                    label.setBackground(new Color(50, 50, 50));
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(Color.WHITE);
                    label.setForeground(new Color(180, 0, 0));
                }

                return label;
            }
        });

        JScrollPane errorScroll = new JScrollPane(errorList);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Preview", codeScroll);
        tabs.addTab("Metadata", metaScroll);
        tabs.addTab("Errors", errorScroll);

        JToolBar toolBar = new JToolBar();
        JButton exportBtn = new JButton("Export Entry");
        exportBtn.addActionListener(e -> exportCurrentEntry());
        toolBar.add(exportBtn);

        wrapToggle.addActionListener(e -> {
            boolean wrap = wrapToggle.isSelected();
            codeArea.setLineWrap(wrap);
            codeArea.setWrapStyleWord(wrap);
        });
        wrapToggle.setSelected(true);
        toolBar.add(wrapToggle);
        add(toolBar, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, tabs);
        splitPane.setDividerLocation(300);
        getContentPane().add(splitPane, BorderLayout.CENTER);

        new DropTarget(this, DnDConstants.ACTION_COPY, new java.awt.dnd.DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                handleDrop(dtde);
            }
        }, true);

        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = e.getPath();
                if (path == null || currentZip == null) return;
                Object last = path.getLastPathComponent();
                if (last instanceof DefaultMutableTreeNode node) {
                    Object userObj = node.getUserObject();
                    if (userObj instanceof ZipEntry entry) {
                        currentEntry = entry;
                        displayEntry(entry);
                    }
                }
            }
        });
    }

    private void handleDrop(DropTargetDropEvent dtde) {
        try {
            dtde.acceptDrop(DnDConstants.ACTION_COPY);
            @SuppressWarnings("unchecked")
            List<File> droppedFiles = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            if (!droppedFiles.isEmpty()) {
                File file = droppedFiles.get(0);
                if (file.getName().toLowerCase().endsWith(".jntc")) {
                    loadZip(file);
                } else {
                    JOptionPane.showMessageDialog(this, "Please drop a .jntc file", "Invalid file", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadZip(File file) {
        try {
            if (currentZip != null) currentZip.close();

            File toOpen = file;
            byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
            if (!(fileBytes[0] == 'P' && fileBytes[1] == 'K')) {
                byte[] iv = Arrays.copyOfRange(fileBytes,0,16);
                byte[] cipher = Arrays.copyOfRange(fileBytes,16,fileBytes.length);
                byte[] key = "JNTC_MASTER_KEY!".getBytes(StandardCharsets.UTF_8);
                byte[] plain = Crypto.decrypt(cipher,key,iv);
                File tmp = File.createTempFile("jntc_decrypted",".zip");
                tmp.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(tmp)) { fos.write(plain); }
                toOpen = tmp;
            }

            currentZip = new ZipFile(toOpen);
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(file.getName());
            currentZip.stream().forEach(entry -> {
                addEntryToTree(root, entry);
            });
            tree.setModel(new DefaultTreeModel(root));
            tree.expandRow(0);
            //setTitle("jntc - " + file.getAbsolutePath());
            codeArea.setText("");
            loadMetadataAndErrors();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to open zip: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addEntryToTree(DefaultMutableTreeNode root, ZipEntry entry) {
        String[] parts = entry.getName().split("/");
        DefaultMutableTreeNode current = root;
        StringBuilder pathBuilder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            pathBuilder.append(part);
            DefaultMutableTreeNode child = findChild(current, part, i == parts.length - 1 ? entry : null);
            current = child;
            pathBuilder.append("/");
        }
    }

    private DefaultMutableTreeNode findChild(DefaultMutableTreeNode parent, String childName, ZipEntry entryObj) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (childName.equals(child.getUserObject().toString())) {
                return child;
            }
        }
        DefaultMutableTreeNode newChild;
        if (entryObj != null) {
            newChild = new DefaultMutableTreeNode(entryObj);
        } else {
            newChild = new DefaultMutableTreeNode(childName);
        }
        parent.add(newChild);
        return newChild;
    }

    private void displayEntry(ZipEntry entry) {
        if (entry.isDirectory()) {
            codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            codeArea.setText("<directory>");
            return;
        }
        try (InputStream is = currentZip.getInputStream(entry)) {
            String name = entry.getName().toLowerCase();
            byte[] data = is.readAllBytes();
            String text;
            if (name.endsWith(".c") || name.endsWith(".h")) {
                text = new String(data, StandardCharsets.UTF_8);
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
            } else if (name.endsWith(".java")) {
                text = new String(data, StandardCharsets.UTF_8);
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
            } else if (name.endsWith(".kt")) {
                text = new String(data, StandardCharsets.UTF_8);
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_KOTLIN);
            } else if (name.endsWith(".json")) {
                text = new String(data, StandardCharsets.UTF_8);
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            } else if (name.endsWith(".log") || name.endsWith(".txt")) {
                text = new String(data, StandardCharsets.UTF_8);
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            } else if (name.toLowerCase().contains("makefile")) {
                text = new String(data, StandardCharsets.UTF_8);
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            } else {
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                codeArea.setText("Binary file (" + data.length + " bytes)");
                return;
            }
            codeArea.setText(text);
            codeArea.setCaretPosition(0);
        } catch (IOException e) {
            codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            codeArea.setText("Failed to read entry: " + e.getMessage());
        }
    }

    private void loadMetadataAndErrors() {
        metaModel.setRowCount(0);
        errorModel.clear();
        if (currentZip == null) return;
        ZipEntry meta = currentZip.getEntry("metadata.json");
        if (meta != null) {
            try (InputStream is = currentZip.getInputStream(meta)) {
                String txt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                for (String line : txt.split("\n")) {
                    line = line.trim();
                    if (line.startsWith("\"") && line.contains(":")) {
                        String[] kv = line.split(":", 2);
//                        String key = kv[0].replaceAll("[\" ,]", "");
//                        String value = kv[1].replaceAll("[\" ,]", "");

                        String key = kv[0].replaceAll("\"", "").trim();
                        String value = kv[1].replaceAll("\"", "").trim();

                        if (!key.equals("errors")) metaModel.addRow(new Object[]{key, value});
                    }
                }
            } catch (IOException ignored) {}
        }
        try {
            if (meta != null) {
                try (InputStream is = currentZip.getInputStream(meta)) {
                    String txt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    boolean inErrors = false;
                    for (String line : txt.split("\n")) {
                        line = line.trim();
                        if (line.startsWith("\"errors\"")) inErrors = true;
                        else if (inErrors) {
                            if (line.startsWith("]")) break;
                            //String err = line.replaceAll("[\" ,]", "");
                            String err = line.replaceAll("\"", "").trim();

                            if(!err.isEmpty()) errorModel.addElement(err);
                        }
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    private void exportCurrentEntry() {
        if (currentEntry == null || currentZip == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(currentEntry.getName().replaceAll("/", "_")));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try (InputStream is = currentZip.getInputStream(currentEntry);
                 FileOutputStream fos = new FileOutputStream(dest)) {
                is.transferTo(fos);
                JOptionPane.showMessageDialog(this, "Exported to " + dest.getAbsolutePath(), "Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to export: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        com.formdev.flatlaf.FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new JNTC().setVisible(true));
    }
}
