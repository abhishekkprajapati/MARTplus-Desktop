package com.martplus.desktop.ui;

import com.martplus.desktop.concurrency.AsyncOperation;
import com.martplus.desktop.model.ActivityLog;
import com.martplus.desktop.service.ActivityService;
import com.martplus.desktop.util.DateUtil;
import com.martplus.desktop.util.DialogUtil;
import com.martplus.desktop.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel displaying Administrative Activity Logs and Audit Trails.
 * Reference: frontend/admin/activity.html
 */
public class ActivityLogPanel extends JPanel {

    private final ActivityService activityService;

    private JTable logTable;
    private DefaultTableModel tableModel;
    private JLabel countBadge;

    public ActivityLogPanel(ActivityService activityService) {
        this.activityService = activityService;

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_LIGHT);

        add(createTopPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);

        loadLogs();
    }

    private JPanel createTopPanel() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel titleLbl = new JLabel("Administrative Activity Audit Trail");
        titleLbl.setFont(UITheme.FONT_SUBTITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        left.add(titleLbl);

        countBadge = UITheme.createPillBadge("0 LOGS", UITheme.PRIMARY_LIGHT, UITheme.PRIMARY);
        left.add(countBadge);

        card.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh Logs");
        refreshBtn.addActionListener(e -> loadLogs());
        right.add(refreshBtn);

        card.add(right, BorderLayout.EAST);

        return card;
    }

    private JPanel createTablePanel() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout());

        String[] cols = {"Log ID", "Administrator", "Action", "Description", "Timestamp"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        logTable = new JTable(tableModel);
        UITheme.styleTable(logTable);
        logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        logTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(350);
        logTable.getColumnModel().getColumn(4).setPreferredWidth(150);

        // Action Column Renderer
        logTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setFont(UITheme.FONT_BODY_BOLD);
                lbl.setForeground(UITheme.PRIMARY);
                return lbl;
            }
        });

        JScrollPane scroll = new JScrollPane(logTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    public void loadLogs() {
        AsyncOperation.run(
                () -> activityService.getRecentLogs(100),
                logs -> {
                    tableModel.setRowCount(0);
                    countBadge.setText(logs.size() + " RECENT LOGS");
                    for (ActivityLog al : logs) {
                        tableModel.addRow(new Object[]{
                                al.getId(),
                                al.getAdminName() != null ? al.getAdminName() : "Admin #" + al.getAdminId(),
                                al.getAction(),
                                al.getDescription(),
                                DateUtil.formatDateTime(al.getCreatedAt())
                        });
                    }
                },
                ex -> DialogUtil.showError(this, "Failed to load activity logs: " + ex.getMessage())
        );
    }
}
