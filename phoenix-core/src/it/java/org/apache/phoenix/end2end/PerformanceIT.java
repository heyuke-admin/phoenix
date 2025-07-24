package org.apache.phoenix.end2end;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.sql.*;
import java.util.Properties;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Category(ParallelStatsDisabledTest.class)
public class PerformanceIT extends ParallelStatsDisabledIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceIT.class);
    private static final long MAX_SQL_TIME_MS = 5000;

    private void assertSqlPerformance(Connection conn, String sql, Object[] params, boolean isQuery) throws Exception {
        long start = System.currentTimeMillis();
        if (isQuery) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        stmt.setObject(i + 1, params[i]);
                    }
                }
                ResultSet rs = stmt.executeQuery();
                int rowCount = 0;
                while (rs.next()) rowCount++;
                long duration = System.currentTimeMillis() - start;
                LOGGER.info("[PERF-TEST] SQL=\"{}\", params={}, duration={}ms, rowCount={}", sql, params, duration, rowCount);
                assertTrue("SQL耗时超出阈值: " + duration + "ms", duration <= MAX_SQL_TIME_MS);
            }
        } else {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        stmt.setObject(i + 1, params[i]);
                    }
                }
                int updateCount = stmt.executeUpdate();
                long duration = System.currentTimeMillis() - start;
                LOGGER.info("[PERF-TEST] SQL=\"{}\", params={}, duration={}ms, updateCount={}", sql, params, duration, updateCount);
                assertTrue("SQL耗时超出阈值: " + duration + "ms", duration <= MAX_SQL_TIME_MS);
            }
        }
    }

    @Test
    public void testPerformance() throws Exception {
        Properties props = new Properties();
        try (Connection conn = DriverManager.getConnection(getUrl(), props)) {
            conn.setAutoCommit(true);
            // 1. CREATE TABLE
            String createTable = "CREATE TABLE IF NOT EXISTS PERF_TEST (ID INTEGER PRIMARY KEY, VAL VARCHAR)";
            assertSqlPerformance(conn, createTable, null, false);
            // 2. INSERT
            for (int i = 1; i <= 10; i++) {
                assertSqlPerformance(conn, "UPSERT INTO PERF_TEST VALUES (?, ?)", new Object[]{i, "val" + i}, false);
            }
            // 3. SELECT
            assertSqlPerformance(conn, "SELECT * FROM PERF_TEST", null, true);
            // 4. UPDATE
            assertSqlPerformance(conn, "UPDATE PERF_TEST SET VAL=? WHERE ID=?", new Object[]{"updated", 1}, false);
            // 5. DELETE
            assertSqlPerformance(conn, "DELETE FROM PERF_TEST WHERE ID=?", new Object[]{1}, false);
            // 6. JOIN
            String createTable2 = "CREATE TABLE IF NOT EXISTS PERF_TEST2 (ID INTEGER PRIMARY KEY, VAL2 VARCHAR)";
            assertSqlPerformance(conn, createTable2, null, false);
            assertSqlPerformance(conn, "UPSERT INTO PERF_TEST2 VALUES (?, ?)", new Object[]{1, "v2"}, false);
            assertSqlPerformance(conn, "SELECT t1.ID, t1.VAL, t2.VAL2 FROM PERF_TEST t1 JOIN PERF_TEST2 t2 ON t1.ID = t2.ID", null, true);
        }
    }
} 