package com.doan.cicdmanager.ui;

public interface Refreshable {

    /** Tải lại dữ liệu từ database. Được gọi mỗi khi người dùng mở màn hình này. */
    void refresh();
}