package com.uddernetworks.newocr.altsearcher;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DatabaseManager {

    private DataSource dataSource;
    private ExecutorService executor = Executors.newFixedThreadPool(25);
    private String createLetterEntry;
    private String clearLetterSegments;
    private String addLetterSegment;
    private String selectSegments;
    private String selectAllSegments;

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
        this.selectSegments = getQuery("selectSegments");
        this.selectAllSegments = getQuery("selectAllSegments");
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

    public Future createLetterEntry(char letter, double averageWidth, double averageHeight, int minFontSize, int maxFontSize) {
        return executor.submit(() -> {
            try (Connection connection = dataSource.getConnection();
                PreparedStatement createLetterEntry = connection.prepareStatement(this.createLetterEntry)) {
                createLetterEntry.setInt(1, letter);
                createLetterEntry.setDouble(2, averageWidth);
                createLetterEntry.setDouble(3, averageHeight);
                createLetterEntry.setInt(4, minFontSize);
                createLetterEntry.setInt(5, maxFontSize);
                createLetterEntry.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public Future clearLetterSegments(char letter) {
        return executor.submit(() -> Arrays.asList("letters", "sectionData").forEach(table -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement clearLetterSegments = connection.prepareStatement(String.format(this.clearLetterSegments, table))) {
                clearLetterSegments.setInt(1, letter);
                clearLetterSegments.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
    }

    public Future addLetterSegments(char letter, double[] segments) {
        return executor.submit(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement addLetterSegment = connection.prepareStatement(this.addLetterSegment)) {
                for (int i = 0; i < segments.length; i++) {
                    addLetterSegment.setInt(1, letter);
                    addLetterSegment.setInt(2, i);
                    addLetterSegment.setDouble(3, segments[i]);
                    addLetterSegment.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public Future<double[]> getCharacterSegments(char character) {
        return executor.submit(() -> {
            double[] doubles = new double[16];
            Arrays.fill(doubles, 0);
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement selectSegments = connection.prepareStatement(this.selectSegments)) {
                selectSegments.setInt(1, character);

                ResultSet resultSet = selectSegments.executeQuery();

                while (resultSet.next()) {
                    int sectionIndex = resultSet.getInt("sectionIndex");
                    double data = resultSet.getDouble("data");

                    doubles[sectionIndex] = data;
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return doubles;
        });
    }

    public Future<Map<Character, double[]>> getAllCharacterSegments() {
        return executor.submit(() -> {
            Map<Character, double[]> ret = new HashMap<>();

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement selectSegments = connection.prepareStatement(this.selectAllSegments)) {

                ResultSet resultSet = selectSegments.executeQuery();

                while (resultSet.next()) {
                    char letter = resultSet.getString("letter").charAt(0);
                    int sectionIndex = resultSet.getInt("sectionIndex");
                    double data = resultSet.getDouble("data");

                    ret.putIfAbsent(letter, new double[17]);
                    ret.get(letter)[sectionIndex] = data;
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return ret;
        });
    }
}
