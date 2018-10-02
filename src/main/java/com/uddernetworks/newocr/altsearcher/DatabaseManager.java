package com.uddernetworks.newocr.altsearcher;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DatabaseManager {

    private DataSource dataSource;
    private ExecutorService executor = Executors.newFixedThreadPool(25);
    private String createLetterEntry;
    private String clearLetterSegments;
    private String addLetterSegment;

    public DatabaseManager(String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "1000");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "8192");
        dataSource = new HikariDataSource(config);
    }

    public void initializeStatements() throws IOException {
        this.createLetterEntry = getQuery("createLetterEntry");
        this.clearLetterSegments = getQuery("clearLetterSegments");
        this.addLetterSegment = getQuery("addLetterSegment");
    }

    private String getQuery(String name) throws IOException {
        return new BufferedReader(new InputStreamReader(Main.class.getClassLoader().getResource(name + ".sql").openStream())).lines().collect(Collectors.joining("\n"));
    }

    private void prepareStatement(String statementName, Consumer<PreparedStatement> preparedStatementConsumer) {
        executor.execute(() -> {
            try (Connection connection = dataSource.getConnection()) {
                preparedStatementConsumer.accept(connection.prepareStatement(getQuery(statementName)));
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }

    public void createLetterEntry(char letter, double averageWidth, double averageHeight, int minFontSize, int maxFontSize, Runnable callback) {
        executor.execute(() -> {
            try (Connection connection = dataSource.getConnection();
                PreparedStatement createLetterEntry = connection.prepareStatement(this.createLetterEntry)) {
                createLetterEntry.setString(1, String.valueOf(letter));
                createLetterEntry.setDouble(2, averageWidth);
                createLetterEntry.setDouble(3, averageHeight);
                createLetterEntry.setInt(4, minFontSize);
                createLetterEntry.setInt(5, maxFontSize);
                createLetterEntry.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (callback != null) callback.run();
        });
    }

    public void clearLetterSegments(char letter, Runnable callback) {
        executor.execute(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement clearLetterSegments = connection.prepareStatement(this.clearLetterSegments)) {
                clearLetterSegments.setString(1, String.valueOf(letter));
                clearLetterSegments.setString(2, String.valueOf(letter));
                clearLetterSegments.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (callback != null) callback.run();
        });
    }

    public void addLetterSegments(char letter, double[] segments, Runnable callback) {
        executor.execute(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement addLetterSegment = connection.prepareStatement(this.addLetterSegment)) {
                for (int i = 0; i < segments.length; i++) {
                    addLetterSegment.setString(1, String.valueOf(letter));
                    addLetterSegment.setInt(2, i);
                    addLetterSegment.setDouble(3, segments[i]);
                    addLetterSegment.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (callback != null) callback.run();
        });
    }
}
