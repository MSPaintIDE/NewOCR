package com.uddernetworks.newocr.altsearcher;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DatabaseManager {

    private DataSource dataSource;

    public DatabaseManager() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("TrainingSQLitePool");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:training.db");
        config.setConnectionTestQuery("SELECT 1");
        config.setMaxLifetime(60000); // 60 Sec
        config.setIdleTimeout(45000); // 45 Sec
        config.setMaximumPoolSize(50); // 50 Connections (including idle connections)
        dataSource = new HikariDataSource(config);
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }
}
