package com.uddernetworks.newocr.database;

import com.uddernetworks.newocr.FontBounds;
import com.uddernetworks.newocr.LetterMeta;
import com.uddernetworks.newocr.Main;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OCRDatabaseManager implements DatabaseManager {

    private DataSource dataSource;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private String createLetterEntry;
    private String clearLetterSegments;
    private String addLetterSegment;
    private String selectSegments;
    private String selectAllSegments;
    private String getLetterEntry;
    private String getSpaceEntry;

    private final AtomicReference<Map<FontBounds, List<DatabaseCharacter>>> databaseCharacterCache = new AtomicReference<>(new HashMap<>());

    /**
     * Connects to the database with the given credentials, and executes the queries found in letters.sql and sectionData.sql
     * @param databaseURL The URL to the database
     * @param username The username of the connecting account
     * @param password The password of the connecting account
     * @throws IOException
     */
    public OCRDatabaseManager(String databaseURL, String username, String password) throws IOException {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setJdbcUrl(databaseURL);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "1000");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "8192");
        dataSource = new HikariDataSource(config);

        Arrays.asList("letters.sql", "sectionData.sql").parallelStream().forEach(table -> {
            try {
                URL url = getClass().getClassLoader().getResource(table);
                String tables = new BufferedReader(new InputStreamReader(url.openStream())).lines().collect(Collectors.joining("\n"));
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(tables)) {

                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        initializeStatements();
    }

    /**
     * Ran internally after the DatabaseManager has been created to read the *.sql files in the /resources/ directory
     * for future queries.
     * @throws IOException
     */
    private void initializeStatements() throws IOException {
        this.createLetterEntry = getQuery("createLetterEntry");
        this.clearLetterSegments = getQuery("clearLetterSegments");
        this.addLetterSegment = getQuery("addLetterSegment");
        this.selectSegments = getQuery("selectSegments");
        this.selectAllSegments = getQuery("selectAllSegments");
        this.getLetterEntry = getQuery("getLetterEntry");
        this.getSpaceEntry = getQuery("getSpaceEntry");
    }

    /**
     * Gets the string query from the resource file given.
     * @param name The resource file to read
     * @return The string contents of it
     * @throws IOException
     */
    private String getQuery(String name) throws IOException {
        return new BufferedReader(new InputStreamReader(Main.class.getClassLoader().getResource(name + ".sql").openStream())).lines().collect(Collectors.joining("\n"));
    }

    @Override
    public DataSource getDataSource() {
        return this.dataSource;
    }

    @Override
    public Future createLetterEntry(char letter, double averageWidth, double averageHeight, int minFontSize, int maxFontSize, double minCenter, double maxCenter, boolean hasDot, LetterMeta letterMeta, boolean isLetter) {
        return executor.submit(() -> {
            try (Connection connection = dataSource.getConnection();
                PreparedStatement createLetterEntry = connection.prepareStatement(this.createLetterEntry)) {
                createLetterEntry.setInt(1, letter);
                createLetterEntry.setDouble(2, averageWidth);
                createLetterEntry.setDouble(3, averageHeight);
                createLetterEntry.setInt(4, minFontSize);
                createLetterEntry.setInt(5, maxFontSize);
                createLetterEntry.setDouble(6, minCenter);
                createLetterEntry.setDouble(7, maxCenter);
                createLetterEntry.setBoolean(8, hasDot);
                createLetterEntry.setInt(9, letterMeta.getID());
                createLetterEntry.setBoolean(10, isLetter);
                createLetterEntry.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Future clearLetterSegments(char letter, int minFontSize, int maxFontSize) {
        return executor.submit(() -> Arrays.asList("letters", "sectionData").forEach(table -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement clearLetterSegments = connection.prepareStatement(String.format(this.clearLetterSegments, table))) {
                clearLetterSegments.setInt(1, letter);
                clearLetterSegments.setInt(2, minFontSize);
                clearLetterSegments.setInt(3, maxFontSize);
                clearLetterSegments.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
    }

    @Override
    public Future addLetterSegments(char letter, int minFontSize, int maxFontSize, double[] segments) {
        return executor.submit(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement addLetterSegment = connection.prepareStatement(this.addLetterSegment)) {
                for (int i = 0; i < segments.length; i++) {
                    addLetterSegment.setInt(1, letter);
                    addLetterSegment.setInt(2, minFontSize);
                    addLetterSegment.setInt(3, maxFontSize);
                    addLetterSegment.setInt(4, i);
                    addLetterSegment.setDouble(5, segments[i]);
                    addLetterSegment.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Future<List<DatabaseCharacter>> getAllCharacterSegments(FontBounds fontBounds) {
        return executor.submit(() -> {
            if (this.databaseCharacterCache.get().get(fontBounds) != null && !this.databaseCharacterCache.get().get(fontBounds).isEmpty()) return this.databaseCharacterCache.get().get(fontBounds);

            List<DatabaseCharacter> databaseCharacters = new ArrayList<>();

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement selectSegments = connection.prepareStatement(this.selectAllSegments)) {
                selectSegments.setInt(1, fontBounds.getMinFont());
                selectSegments.setInt(2, fontBounds.getMaxFont());

                ResultSet resultSet = selectSegments.executeQuery();

                while (resultSet.next()) {
                    char letter = resultSet.getString("letter").charAt(0);
                    int sectionIndex = resultSet.getInt("sectionIndex");
                    double data = resultSet.getDouble("data");

                    DatabaseCharacter databaseCharacter = getDatabaseCharacter(databaseCharacters, letter, newDatabaseCharacter -> {
                        try (PreparedStatement getLetterEntry = connection.prepareCall(this.getLetterEntry)) {
                            getLetterEntry.setInt(1, letter);
                            getLetterEntry.setInt(2, fontBounds.getMinFont());
                            getLetterEntry.setInt(3, fontBounds.getMaxFont());
                            ResultSet resultSet1 = getLetterEntry.executeQuery();

                            if (!resultSet1.next()) return;

                            double avgWidth = resultSet1.getDouble("avgWidth");
                            double avgHeight = resultSet1.getDouble("avgHeight");
                            int minFontSize = resultSet1.getInt("minFontSize");
                            int maxFontSize = resultSet1.getInt("maxFontSize");
                            double minCenter = resultSet1.getDouble("minCenter");
                            double maxCenter = resultSet1.getDouble("maxCenter");
                            boolean hasDot = resultSet1.getBoolean("hasDot");
                            int letterMetaID = resultSet1.getInt("letterMeta");

                            newDatabaseCharacter.setData(avgWidth, avgHeight, minFontSize, maxFontSize, minCenter, maxCenter);
                            newDatabaseCharacter.setHasDot(hasDot);
                            newDatabaseCharacter.setLetterMeta(LetterMeta.fromID(letterMetaID));
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });

                    databaseCharacter.addDataPoint(sectionIndex, data);
                    if (!databaseCharacters.contains(databaseCharacter)) databaseCharacters.add(databaseCharacter);
                }

                try (PreparedStatement selectSpace = connection.prepareStatement(this.getSpaceEntry)) {
                    ResultSet spaceResult = selectSpace.executeQuery();

                    if (spaceResult.next()) {

                        double avgWidth = spaceResult.getDouble("avgWidth");
                        double avgHeight = spaceResult.getDouble("avgHeight");
                        int minFontSize = spaceResult.getInt("minFontSize");
                        int maxFontSize = spaceResult.getInt("maxFontSize");

                        DatabaseCharacter spaceCharacter = new DatabaseCharacter(' ');
                        spaceCharacter.setData(avgWidth, avgHeight, minFontSize, maxFontSize, 0, 0);
                        databaseCharacters.add(spaceCharacter);
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            this.databaseCharacterCache.get().put(fontBounds, databaseCharacters);

            return databaseCharacters;
        });
    }

    /**
     * Gets the {@link DatabaseCharacter} with the character value given from a list of {@link DatabaseCharacter}s.
     * If one is not found, one is created.
     * @param databaseCharacters The list of {@link DatabaseCharacter}s to search from
     * @param letter The character the value must match
     * @param onCreate An action to do when a {@link DatabaseCharacter} is created, usually adding more info from it
     *                 from a database.
     * @return The created {@link DatabaseCharacter}
     */
    private DatabaseCharacter getDatabaseCharacter(List<DatabaseCharacter> databaseCharacters, char letter, Consumer<DatabaseCharacter> onCreate) {
        return databaseCharacters.stream().filter(cha -> cha.getLetter() == letter).findFirst().orElseGet(() -> {
            DatabaseCharacter databaseCharacter = new DatabaseCharacter(letter);
            onCreate.accept(databaseCharacter);
            return databaseCharacter;
        });
    }
}