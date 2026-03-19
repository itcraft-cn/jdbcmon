package cn.itcraft.jdbcmon.wrap;

import java.sql.SQLException;

interface ResultSetMonitor {

    void onRow() throws SQLException;

    int onClose();

    ResultSetMonitor NOOP = new ResultSetMonitor() {
        @Override
        public void onRow() {}

        @Override
        public int onClose() { return 0; }
    };
}