package com.personaltracker.view;

import com.personaltracker.components.ModernButton;
import com.personaltracker.components.ModernCard;
import com.personaltracker.components.ModernTextField;
import com.personaltracker.components.ToastNotification;
import com.personaltracker.config.ThemeConfig;
import com.personaltracker.dao.PhotoDAO;
import com.personaltracker.model.ProgressPhoto;
import com.personaltracker.util.DateUtil;
import com.personaltracker.util.IconFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Progress Photos management and gallery view.
 */
public class PhotoPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final PhotoDAO photoDAO = new PhotoDAO();
    private final JPanel pnlGallery = new JPanel();
    private List<ProgressPhoto> currentPhotos = new ArrayList<>();

    public PhotoPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("Progress Photos");
        title.setFont(ThemeConfig.FONT_TITLE_LARGE);
        title.setForeground(ThemeConfig.TEXT_PRIMARY);
        titlePanel.add(title);

        JLabel subtitle = new JLabel("Visual physique transformation log with timestamps and notes");
        subtitle.setFont(ThemeConfig.FONT_BODY);
        subtitle.setForeground(ThemeConfig.TEXT_SECONDARY);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        ModernButton btnUpload = new ModernButton("+ Upload Photo", ModernButton.ButtonStyle.PRIMARY);
        btnUpload.addActionListener(e -> openUploadDialog());
        header.add(btnUpload, BorderLayout.EAST);

        content.add(header);
        content.add(Box.createVerticalStrut(20));

        // Photos Grid
        pnlGallery.setLayout(new GridLayout(0, 3, 16, 16));
        pnlGallery.setOpaque(false);
        content.add(pnlGallery);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadPhotos() {
        SwingWorker<List<ProgressPhoto>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ProgressPhoto> doInBackground() throws Exception {
                return photoDAO.getAllPhotos();
            }

            @Override
            protected void done() {
                try {
                    currentPhotos = get();
                    updateGalleryView();
                } catch (Exception e) {
                    System.err.println("Failed to load photos: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateGalleryView() {
        pnlGallery.removeAll();

        for (ProgressPhoto p : currentPhotos) {
            ModernCard card = new ModernCard(14, new Insets(14, 14, 14, 14));
            card.setHoverEffect(true);

            // Thumbnail panel
            JPanel thumbPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(ThemeConfig.BG_INPUT);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                }
            };
            thumbPanel.setOpaque(false);
            thumbPanel.setPreferredSize(new Dimension(0, 160));

            if (p.getImageBytes() != null && p.getImageBytes().length > 0) {
                try {
                    ImageIcon imgIcon = new ImageIcon(p.getImageBytes());
                    Image scaled = imgIcon.getImage().getScaledInstance(220, 160, Image.SCALE_SMOOTH);
                    thumbPanel.add(new JLabel(new ImageIcon(scaled)), BorderLayout.CENTER);
                } catch (Exception ex) {
                    thumbPanel.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.PHOTOS, 48, ThemeConfig.PRIMARY_ACCENT)), BorderLayout.CENTER);
                }
            } else {
                thumbPanel.add(new JLabel(IconFactory.createIcon(IconFactory.IconType.PHOTOS, 48, ThemeConfig.PRIMARY_ACCENT)), BorderLayout.CENTER);
            }
            card.add(thumbPanel, BorderLayout.NORTH);

            JPanel desc = new JPanel();
            desc.setLayout(new BoxLayout(desc, BoxLayout.Y_AXIS));
            desc.setOpaque(false);
            desc.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

            JLabel dateLbl = new JLabel(DateUtil.formatDate(p.getPhotoDate()));
            dateLbl.setFont(ThemeConfig.FONT_BODY_BOLD);
            dateLbl.setForeground(ThemeConfig.TEXT_PRIMARY);
            desc.add(dateLbl);
            desc.add(Box.createVerticalStrut(4));

            JLabel noteLbl = new JLabel(p.getNotes() != null && !p.getNotes().isEmpty() ? p.getNotes() : "No notes recorded");
            noteLbl.setFont(ThemeConfig.FONT_SMALL);
            noteLbl.setForeground(ThemeConfig.TEXT_MUTED);
            desc.add(noteLbl);
            card.add(desc, BorderLayout.CENTER);

            // Delete button
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            bottom.setOpaque(false);
            JButton btnDel = new JButton("Delete");
            btnDel.setFont(ThemeConfig.FONT_SMALL);
            btnDel.setForeground(ThemeConfig.DANGER_ROSE);
            btnDel.setContentAreaFilled(false);
            btnDel.setBorderPainted(false);
            btnDel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDel.addActionListener(e -> deletePhoto(p));
            bottom.add(btnDel);
            card.add(bottom, BorderLayout.SOUTH);

            pnlGallery.add(card);
        }

        if (currentPhotos.isEmpty()) {
            ModernCard emptyCard = new ModernCard(14, new Insets(24, 20, 24, 20));
            JLabel lblEmpty = new JLabel("No progress photos uploaded yet. Click '+ Upload Photo' to record your transformation!");
            lblEmpty.setFont(ThemeConfig.FONT_BODY);
            lblEmpty.setForeground(ThemeConfig.TEXT_MUTED);
            emptyCard.add(lblEmpty, BorderLayout.CENTER);
            pnlGallery.add(emptyCard);
        }

        pnlGallery.revalidate();
        pnlGallery.repaint();
    }

    private void deletePhoto(ProgressPhoto photo) {
        int confirm = JOptionPane.showConfirmDialog(this, "Delete progress photo?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    photoDAO.deletePhoto(photo.getId());
                    return null;
                }
                @Override
                protected void done() {
                    loadPhotos();
                }
            };
            worker.execute();
        }
    }

    private void openUploadDialog() {
        UploadDialog dialog = new UploadDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadPhotos();
        }
    }

    // Modal Upload Dialog
    private class UploadDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        private boolean saved = false;
        private File selectedFile;

        private final JLabel lblFile = new JLabel("No image chosen (PNG, JPG)");
        private final ModernTextField txtDate = new ModernTextField(LocalDate.now().toString());
        private final ModernTextField txtNotes = new ModernTextField("Front pose check-in");

        public UploadDialog(Window owner) {
            super(owner, "Upload Progress Photo", ModalityType.APPLICATION_MODAL);
            setSize(490, 520);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(ThemeConfig.BG_DARK);

            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setOpaque(false);
            main.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

            // Section 1: Choose File
            JLabel lblChoose = new JLabel("Image File * (PNG, JPG)");
            lblChoose.setFont(ThemeConfig.FONT_SMALL);
            lblChoose.setForeground(ThemeConfig.TEXT_SECONDARY);
            main.add(lblChoose);
            main.add(Box.createVerticalStrut(4));

            JPanel fileRow = new JPanel(new BorderLayout(8, 0));
            fileRow.setOpaque(false);
            lblFile.setFont(ThemeConfig.FONT_BODY);
            lblFile.setForeground(ThemeConfig.TEXT_PRIMARY);
            fileRow.add(lblFile, BorderLayout.CENTER);

            ModernButton btnBrowse = new ModernButton("Browse Image...", ModernButton.ButtonStyle.SECONDARY);
            btnBrowse.addActionListener(e -> chooseFile());
            fileRow.add(btnBrowse, BorderLayout.EAST);
            main.add(fileRow);
            main.add(Box.createVerticalStrut(14));

            // Section 2: Date with Quick Pills
            JPanel dateRow = new JPanel(new BorderLayout(8, 0));
            dateRow.setOpaque(false);
            JLabel lblDateTag = new JLabel("Photo Date");
            lblDateTag.setFont(ThemeConfig.FONT_SMALL);
            lblDateTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            dateRow.add(lblDateTag, BorderLayout.NORTH);

            JPanel dateControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            dateControls.setOpaque(false);
            JButton btnToday = createQuickPill("Today", true);
            JButton btnYesterday = createQuickPill("Yesterday", false);
            btnToday.addActionListener(e -> {
                txtDate.setText(LocalDate.now().toString());
                btnToday.setBackground(ThemeConfig.PRIMARY_ACCENT);
                btnYesterday.setBackground(ThemeConfig.BG_CARD);
            });
            btnYesterday.addActionListener(e -> {
                txtDate.setText(LocalDate.now().minusDays(1).toString());
                btnYesterday.setBackground(ThemeConfig.PRIMARY_ACCENT);
                btnToday.setBackground(ThemeConfig.BG_CARD);
            });
            txtDate.setPreferredSize(new Dimension(130, 32));
            dateControls.add(btnToday);
            dateControls.add(btnYesterday);
            dateControls.add(txtDate);
            dateRow.add(dateControls, BorderLayout.CENTER);
            main.add(dateRow);
            main.add(Box.createVerticalStrut(14));

            // Section 3: Pose Quick Chips & Notes
            JPanel notesSection = new JPanel();
            notesSection.setLayout(new BoxLayout(notesSection, BoxLayout.Y_AXIS));
            notesSection.setOpaque(false);

            JLabel lblPoseTag = new JLabel("Quick Pose Tags / Notes");
            lblPoseTag.setFont(ThemeConfig.FONT_SMALL);
            lblPoseTag.setForeground(ThemeConfig.TEXT_SECONDARY);
            notesSection.add(lblPoseTag);
            notesSection.add(Box.createVerticalStrut(4));

            JPanel poseChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            poseChips.setOpaque(false);
            poseChips.add(createPoseChip("📸 Front Relaxed"));
            poseChips.add(createPoseChip("💪 Front Double Bicep"));
            poseChips.add(createPoseChip("🔙 Back Lat Spread"));
            poseChips.add(createPoseChip("✨ Side Profile"));
            notesSection.add(poseChips);
            notesSection.add(Box.createVerticalStrut(6));
            notesSection.add(txtNotes);
            main.add(notesSection);

            main.add(Box.createVerticalGlue());
            main.add(Box.createVerticalStrut(16));

            // Section 4: Action Buttons
            JPanel actions = new JPanel(new BorderLayout());
            actions.setOpaque(false);
            JLabel lblHint = new JLabel("Tip: Press Enter to upload");
            lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblHint.setForeground(ThemeConfig.TEXT_MUTED);
            actions.add(lblHint, BorderLayout.WEST);

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btnRow.setOpaque(false);
            ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonStyle.GHOST);
            btnCancel.addActionListener(e -> dispose());
            ModernButton btnSave = new ModernButton("Upload Photo", ModernButton.ButtonStyle.PRIMARY);
            btnSave.addActionListener(e -> savePhoto());
            btnRow.add(btnCancel);
            btnRow.add(btnSave);
            actions.add(btnRow, BorderLayout.EAST);
            main.add(actions);

            setContentPane(main);

            // Shortcuts
            txtDate.addActionListener(e -> savePhoto());
            txtNotes.addActionListener(e -> savePhoto());

            getRootPane().registerKeyboardAction(e -> dispose(),
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        private JButton createPoseChip(String text) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            btn.addActionListener(e -> txtNotes.setText(text));
            return btn;
        }

        private JButton createQuickPill(String text, boolean active) {
            JButton btn = new JButton(text);
            btn.setFont(ThemeConfig.FONT_SMALL);
            btn.setForeground(ThemeConfig.TEXT_PRIMARY);
            btn.setBackground(active ? ThemeConfig.PRIMARY_ACCENT : ThemeConfig.BG_CARD);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            return btn;
        }

        private JPanel createFieldRow(String label, JComponent comp) {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setOpaque(false);
            JLabel l = new JLabel(label);
            l.setFont(ThemeConfig.FONT_SMALL);
            l.setForeground(ThemeConfig.TEXT_SECONDARY);
            p.add(l);
            p.add(Box.createVerticalStrut(4));
            p.add(comp);
            return p;
        }

        private void chooseFile() {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Image Files (*.jpg, *.png, *.jpeg)", "jpg", "png", "jpeg"));
            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                selectedFile = chooser.getSelectedFile();
                lblFile.setText(selectedFile.getName());
            }
        }

        private void savePhoto() {
            if (selectedFile == null) {
                JOptionPane.showMessageDialog(this, "Please choose an image file first.");
                return;
            }

            LocalDate date = DateUtil.parseDateOrNull(txtDate.getText().trim());
            if (date == null) {
                JOptionPane.showMessageDialog(this, "Please enter a valid date.");
                return;
            }

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    byte[] bytes;
                    try (FileInputStream fis = new FileInputStream(selectedFile)) {
                        bytes = fis.readAllBytes();
                    }

                    ProgressPhoto photo = new ProgressPhoto(selectedFile.getName(), "progress_photos/" + selectedFile.getName(), date, txtNotes.getText().trim());
                    photo.setImageBytes(bytes);
                    photoDAO.savePhoto(photo);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        saved = true;
                        ToastNotification.showToast(PhotoPanel.this, "Photo uploaded successfully!", ToastNotification.ToastType.SUCCESS);
                        dispose();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(UploadDialog.this, "Upload failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }

        public boolean isSaved() {
            return saved;
        }
    }
}
