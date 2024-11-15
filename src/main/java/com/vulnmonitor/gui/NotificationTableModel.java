package com.vulnmonitor.gui;

import com.vulnmonitor.model.UserNotification;

import javax.swing.table.AbstractTableModel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class NotificationTableModel extends AbstractTableModel {
    private final String[] columnNames = {"ID", "CVE ID", "Message", "Sent At"};
    private final List<UserNotification> notifications = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void setNotifications(List<UserNotification> notifications) {
        this.notifications.clear();
        if (notifications != null) {
            this.notifications.addAll(notifications);
        }
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return notifications.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }
    
    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        UserNotification notification = notifications.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return notification.getId();
            case 1:
                return notification.getCveId();
            case 2:
                return notification.getMessage();
            case 3:
                return dateFormat.format(notification.getSentAt());
            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (notifications.isEmpty()) {
            return Object.class;
        }
        return getValueAt(0, columnIndex).getClass();
    }
}