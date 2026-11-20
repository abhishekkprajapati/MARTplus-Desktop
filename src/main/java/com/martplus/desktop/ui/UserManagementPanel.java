package com.martplus.desktop.ui;

import com.martplus.desktop.concurrency.AsyncOperation;
import com.martplus.desktop.model.User;
import com.martplus.desktop.service.UserService;
import com.martplus.desktop.util.DateUtil;
import com.martplus.desktop.util.DialogUtil;
import com.martplus.desktop.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for Administrative User Management.
 * Security: Password hashes are strictly concealed and never displayed in GUI.
 * Reference: frontend/admin/users.html
 */
public class UserManagementPanel extends JPanel {

    private final UserService userService;
    private final User currentUser;

    private JTable userTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> roleFilterCombo;
    private JLabel countBadge;
    private List<User> userList = new ArrayList<>();

    public UserManagementPanel(UserService userService, User currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_LIGHT);

        add(createTopFilterPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createBottomActionPanel(), BorderLayout.SOUTH);

        loadUsers();
    }

    private JPanel createTopFilterPanel() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel titleLbl = new JLabel("System User Directory");
        titleLbl.setFont(UITheme.FONT_SUBTITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        left.add(titleLbl);

        countBadge = UITheme.createPillBadge("0 USERS", UITheme.PRIMARY_LIGHT, UITheme.PRIMARY);
        left.add(countBadge);

        card.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JLabel filterLbl = new JLabel("Role Filter:");
        filterLbl.setFont(UITheme.FONT_BODY_BOLD);
        filterLbl.setForeground(UITheme.TEXT_SECONDARY);
        right.add(filterLbl);

        roleFilterCombo = new JComboBox<>(new String[]{"ALL", "BUYER", "SELLER", "ADMIN"});
        roleFilterCombo.setFont(UITheme.FONT_BODY);
        roleFilterCombo.addActionListener(e -> loadUsers());
        right.add(roleFilterCombo);

        JButton addUserBtn = UITheme.createPrimaryButton("+ Create New User");
        addUserBtn.addActionListener(e -> handleCreateUser());
        right.add(addUserBtn);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadUsers());
        right.add(refreshBtn);

        card.add(right, BorderLayout.EAST);

        return card;
    }

    private JPanel createTablePanel() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout());

        // Note: Password hashes are never exposed in user management table
        String[] cols = {"User ID", "Full Name", "Email", "Phone", "Role", "Status", "Email Verified", "Created On"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        userTable = new JTable(tableModel);
        UITheme.styleTable(userTable);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        userTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(210);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        userTable.getColumnModel().getColumn(4).setPreferredWidth(85);
        userTable.getColumnModel().getColumn(5).setPreferredWidth(85);
        userTable.getColumnModel().getColumn(6).setPreferredWidth(95);
        userTable.getColumnModel().getColumn(7).setPreferredWidth(130);

        // Role Column Renderer
        userTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String role = value != null ? value.toString() : "";
                lbl.setFont(UITheme.FONT_BODY_BOLD);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                if ("ADMIN".equalsIgnoreCase(role)) {
                    lbl.setForeground(new Color(0x7C, 0x3A, 0xED)); // Purple
                } else if ("SELLER".equalsIgnoreCase(role)) {
                    lbl.setForeground(UITheme.INFO_BLUE);
                } else {
                    lbl.setForeground(UITheme.TEXT_SECONDARY);
                }
                return lbl;
            }
        });

        // Status Column Renderer
        userTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value != null ? value.toString() : "";
                lbl.setFont(UITheme.FONT_BODY_BOLD);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                if ("ACTIVE".equalsIgnoreCase(status)) {
                    lbl.setForeground(UITheme.SUCCESS_GREEN);
                } else {
                    lbl.setForeground(UITheme.DANGER_RED);
                }
                return lbl;
            }
        });

        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel createBottomActionPanel() {
        JPanel card = UITheme.createCard();
        card.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 0));

        JButton changeRoleBtn = UITheme.createSecondaryButton("Modify User Role");
        changeRoleBtn.addActionListener(e -> handleChangeRole());
        card.add(changeRoleBtn);

        JButton toggleStatusBtn = UITheme.createDangerButton("Toggle Active / Inactive");
        toggleStatusBtn.addActionListener(e -> handleToggleStatus());
        card.add(toggleStatusBtn);

        return card;
    }

    public void triggerAddUser() {
        handleCreateUser();
    }

    public void loadUsers() {
        String role = (String) roleFilterCombo.getSelectedItem();
        AsyncOperation.run(
                () -> userService.getUsersByRole(currentUser, role),
                list -> {
                    userList = list;
                    tableModel.setRowCount(0);
                    countBadge.setText(list.size() + " USERS");
                    for (User u : list) {
                        tableModel.addRow(new Object[]{
                                u.getId(),
                                u.getFullName(),
                                u.getEmail(),
                                u.getPhone() != null ? u.getPhone() : "-",
                                u.getRole(),
                                u.getStatus(),
                                u.isEmailVerified() ? "Yes" : "No",
                                DateUtil.formatDateOnly(u.getCreatedAt())
                        });
                    }
                },
                ex -> DialogUtil.showError(this, "Failed to load users: " + ex.getMessage())
        );
    }

    private void handleCreateUser() {
        JTextField firstField = new JTextField();
        JTextField lastField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField phoneField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"BUYER", "SELLER", "ADMIN"});

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("First Name:")); form.add(firstField);
        form.add(new JLabel("Last Name:")); form.add(lastField);
        form.add(new JLabel("Email Address:")); form.add(emailField);
        form.add(new JLabel("Phone Number:")); form.add(phoneField);
        form.add(new JLabel("Password:")); form.add(passField);
        form.add(new JLabel("Role:")); form.add(roleCombo);

        int result = JOptionPane.showConfirmDialog(this, form, "Create System User Account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String email = emailField.getText().trim();
            String pass = new String(passField.getPassword());
            String role = (String) roleCombo.getSelectedItem();

            AsyncOperation.run(
                    () -> userService.createUser(currentUser,
                            firstField.getText().trim(),
                            lastField.getText().trim(),
                            email,
                            pass,
                            phoneField.getText().trim(),
                            role),
                    user -> {
                        DialogUtil.showSuccess(this, "User " + user.getEmail() + " created successfully!");
                        loadUsers();
                    },
                    ex -> DialogUtil.showError(this, ex.getMessage())
            );
        }
    }

    private void handleChangeRole() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            DialogUtil.showWarning(this, "Please select a user from the table first.");
            return;
        }

        User u = userList.get(row);
        String[] options = {"BUYER", "SELLER", "ADMIN"};
        String newRole = (String) JOptionPane.showInputDialog(this,
                "Select new role for user " + u.getFullName() + ":",
                "Change User Role", JOptionPane.PLAIN_MESSAGE, null, options, u.getRole());

        if (newRole != null && !newRole.equals(u.getRole())) {
            AsyncOperation.run(
                    () -> {
                        userService.updateUserRole(currentUser, u.getId(), newRole);
                        return null;
                    },
                    res -> {
                        DialogUtil.showSuccess(this, "Role updated to " + newRole);
                        loadUsers();
                    },
                    ex -> DialogUtil.showError(this, ex.getMessage())
            );
        }
    }

    private void handleToggleStatus() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            DialogUtil.showWarning(this, "Please select a user from the table first.");
            return;
        }

        User u = userList.get(row);
        if (u.getId().equals(currentUser.getId())) {
            DialogUtil.showWarning(this, "You cannot modify your own administrative account status.");
            return;
        }

        String targetStatus = "ACTIVE".equalsIgnoreCase(u.getStatus()) ? "INACTIVE" : "ACTIVE";
        AsyncOperation.run(
                () -> {
                    userService.updateUserStatus(currentUser, u.getId(), targetStatus);
                    return null;
                },
                res -> {
                    DialogUtil.showSuccess(this, "User status updated to " + targetStatus);
                    loadUsers();
                },
                ex -> DialogUtil.showError(this, ex.getMessage())
        );
    }
}
