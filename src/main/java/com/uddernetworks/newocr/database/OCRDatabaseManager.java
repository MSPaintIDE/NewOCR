package com.uddernetworks.newocr.database;

import com.uddernetworks.newocr.LetterMeta;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OCRDatabaseManager implements DatabaseManager {

    private final boolean useInternal;
    private DataSource dataSource;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private String createLetterEntry;
    private String clearLetterSegments;
    private String addLetterSegment;
    private String selectAllSegments;
    private String getLetterEntry;
    private String getSpaceEntry;
    private String addAverageData;
    private String getAverageData;

    private final AtomicReference<List<DatabaseCharacter>> databaseCharacterCache = new AtomicReference<>();

    /**
     * Connects to the database with the given credentials, and executes the queries found in letters.sql and sectionData.sql
     *
     * @param databaseURL The URL to the database
     * @param username    The username of the connecting account
     * @param password    The password of the connecting account
     * @throws IOException
     */
    public OCRDatabaseManager(String databaseURL, String username, String password) throws IOException {
        this(false, null, databaseURL, username, password);
    }

    /**
     * Connects to the internal database provided by HSQLDB in the given location, and executes the queries found in
     * letters.sql and sectionData.sql. This option can be over 12x faster than the MySQL variant.
     *
     * @param filePath The file without an extension of the database. If this doesn't exist, it will be created
     * @throws IOException
     */
    public OCRDatabaseManager(File filePath) throws IOException {
        this(true, filePath, null, null, null);
    }

    public OCRDatabaseManager(boolean useInternal, File filePath, String databaseURL, String username, String password) throws IOException {
        this.useInternal = useInternal;

        var config = new HikariConfig();

        try {
            Class.forName(useInternal ? "org.hsqldb.jdbcDriver" : "com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (useInternal) {
            filePath.getParentFile().mkdirs();
            config.setJdbcUrl("jdbc:hsqldb:file:" + filePath);
            config.setUsername("SA");
            config.setPassword("");
        } else {
            config.setDriverClassName("com.mysql.jdbc.Driver");
            config.setJdbcUrl(databaseURL);
            config.setUsername(username);
            config.setPassword(password);
        }

        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "1000");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "8192");

        dataSource = new HikariDataSource(config);

        List.of("letters.sql", "sectionData.sql", "data.sql").parallelStream().forEach(table -> {
            var stream = OCRDatabaseManager.class.getResourceAsStream("/" + table);

            try (var reader = new BufferedReader(new InputStreamReader(stream));
                 var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(reader.lines().collect(Collectors.joining("\n")))) {
                statement.executeUpdate();
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        });

        initializeStatements();

        try (var connection = this.dataSource.getConnection();
             var useMySQL = connection.prepareStatement("SET DATABASE SQL SYNTAX MYS TRUE")) {
            useMySQL.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ran internally after the DatabaseManager has been created to read the *.sql files in the /resources/ directory
     * for future queries.
     *
     * @throws IOException
     */
    private void initializeStatements() throws IOException {
        this.createLetterEntry = getQuery("createLetterEntry");
        this.clearLetterSegments = getQuery("clearLetterSegments");
        this.addLetterSegment = getQuery("addLetterSegment");
        this.selectAllSegments = getQuery("selectAllSegments");
        this.getLetterEntry = getQuery("getLetterEntry");
        this.getSpaceEntry = getQuery("getSpaceEntry");
        this.addAverageData = getQuery("addAverageData");
        this.getAverageData = getQuery("getAverageData");
    }

    /**
     * Gets the string query from the resource file given.
     *
     * @param name The resource file to read
     * @return The string contents of it
     * @throws IOException
     */
    private String getQuery(String name) throws IOException {
        var resource = Objects.requireNonNull(getClass().getClassLoader().getResource(name + ".sql"));

        try (var reader = new BufferedReader(new InputStreamReader(resource.openStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    @Override
    public DataSource getDataSource() {
        return this.dataSource;
    }

    @Override
    public void createLetterEntry(char letter, double averageWidth, double averageHeight, double minCenter, double maxCenter, boolean hasDot, LetterMeta letterMeta, boolean isLetter) {
        createLetterEntry(letter, 0, averageWidth, averageHeight, minCenter, maxCenter, hasDot, letterMeta, isLetter);
    }

    @Override
    public void createLetterEntry(char letter, int modifier, double averageWidth, double averageHeight, double minCenter, double maxCenter, boolean hasDot, LetterMeta letterMeta, boolean isLetter) {
        try (var connection = dataSource.getConnection(); var createLetterEntry = connection.prepareStatement(this.createLetterEntry)) {
            createLetterEntry.setInt(1, letter);
            createLetterEntry.setInt(2, modifier);
            createLetterEntry.setDouble(3, averageWidth);
            createLetterEntry.setDouble(4, averageHeight);
            createLetterEntry.setDouble(5, minCenter);
            createLetterEntry.setDouble(6, maxCenter);
            createLetterEntry.setBoolean(7, hasDot);
            createLetterEntry.setInt(8, letterMeta.getID());
            createLetterEntry.setBoolean(9, isLetter);
            createLetterEntry.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clearLetterSegments(char letter) {
        Stream.of("letters", "sectionData").parallel().forEach(table -> {
            var query = String.format(this.clearLetterSegments, table);

            try (var connection = dataSource.getConnection(); var clearLetterSegments = connection.prepareStatement(query)) {
                clearLetterSegments.setInt(1, letter);
                clearLetterSegments.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void addLetterSegments(char letter, double[] segments) {
        addLetterSegments(letter, 0, segments);
    }

    @Override
    public void addLetterSegments(char letter, int modifier, double[] segments) {
        try (var connection = dataSource.getConnection(); var addLetterSegment = connection.prepareStatement(this.addLetterSegment)) {
            for (int i = 0; i < segments.length; i++) {
                addLetterSegment.setInt(1, letter);
                addLetterSegment.setInt(2, modifier);
                addLetterSegment.setInt(3, i);
                addLetterSegment.setDouble(4, segments[i]);
                addLetterSegment.addBatch();
            }

            addLetterSegment.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Future<List<DatabaseCharacter>> getAllCharacterSegments() {
        return executor.submit(() -> {
            var cachedValue = databaseCharacterCache.get();

            if (cachedValue != null && !cachedValue.isEmpty()) {
                return cachedValue;
            }

            var databaseCharacters = new ArrayList<DatabaseCharacter>();

            try (var connection = dataSource.getConnection(); var selectSegments = connection.prepareStatement(this.selectAllSegments)) {
                var resultSet = selectSegments.executeQuery();

                while (resultSet.next()) {
                    var letter = resultSet.getString("letter").charAt(0);
                    var modifier = resultSet.getInt("modifier");
                    var sectionIndex = resultSet.getInt("sectionIndex");
                    var data = resultSet.getDouble("data");

                    var databaseCharacter = getDatabaseCharacter(databaseCharacters, letter, modifier, newDatabaseCharacter -> {
                        try (var getLetterEntry = connection.prepareCall(this.getLetterEntry)) {
                            getLetterEntry.setInt(1, letter);
                            getLetterEntry.setInt(2, modifier);

                            var resultSet1 = getLetterEntry.executeQuery();

                            if (!resultSet1.next()) {
                                return;
                            }

                            var avgWidth = resultSet1.getDouble("avgWidth");
                            var avgHeight = resultSet1.getDouble("avgHeight");
                            var minCenter = resultSet1.getDouble("minCenter");
                            var maxCenter = resultSet1.getDouble("maxCenter");
                            var hasDot = resultSet1.getBoolean("hasDot");
                            var letterMetaID = resultSet1.getInt("letterMeta");

                            newDatabaseCharacter.setData(avgWidth, avgHeight, minCenter, maxCenter);
                            newDatabaseCharacter.setHasDot(hasDot);
                            LetterMeta.fromID(letterMetaID).ifPresent(newDatabaseCharacter::setLetterMeta);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });

                    databaseCharacter.addDataPoint(sectionIndex, data);

                    if (!databaseCharacters.contains(databaseCharacter)) {
                        databaseCharacters.add(databaseCharacter);
                    }
                }

                try (var selectSpace = connection.prepareStatement(this.getSpaceEntry)) {
                    var spaceResult = selectSpace.executeQuery();

                    if (spaceResult.next()) {
                        var avgWidth = spaceResult.getDouble("avgWidth");
                        var avgHeight = spaceResult.getDouble("avgHeight");

                        var spaceCharacter = new DatabaseCharacter(' ');
                        spaceCharacter.setData(avgWidth, avgHeight, 0, 0);
                        databaseCharacters.add(spaceCharacter);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            this.databaseCharacterCache.set(databaseCharacters);

            return databaseCharacters;
        });
    }

    @Override
    public void addAveragedData(String name, double[] values) {
        try (var connection = dataSource.getConnection();
             var addData = connection.prepareStatement(this.addAverageData)) {
            for (double value : values) {
                addData.setString(1, name);
                addData.setDouble(2, value);
                addData.addBatch();
            }

            addData.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Future<Double> getAveragedData(String name) {
        return executor.submit(() -> {
            try (var connection = dataSource.getConnection(); var getData = connection.prepareStatement(this.getAverageData)) {
                getData.setString(1, name);
                var resultSet = getData.executeQuery();
                if (!resultSet.next()) return 0D;

                return resultSet.getDouble(1);
            }
        });
    }

    @Override
    public void clearData() {
        Stream.of("letters", "sectionData", "data").parallel().forEach(table -> {
            try (var connection = dataSource.getConnection(); // Keeping the same connection throughout all tables might be faster
                 var truncate = connection.prepareStatement("TRUNCATE TABLE " + table)) {
                truncate.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void shutdown() {
        if (!this.executor.isShutdown()) {
            this.executor.shutdown();
        }
    }

    @Override
    public boolean usesInternal() {
        return this.useInternal;
    }

    /**
     * Gets the {@link DatabaseCharacter} with the character value given from a list of {@link DatabaseCharacter}s.
     * If one is not found, one is created.
     *
     * @param databaseCharacters The list of {@link DatabaseCharacter}s to search from
     * @param letter             The character the value must match
     * @param modifier           The modifier of the character
     * @param onCreate           An action to do when a {@link DatabaseCharacter} is created, usually adding more info from it
     *                           from a database.
     * @return The created {@link DatabaseCharacter}
     */
    private DatabaseCharacter getDatabaseCharacter(List<DatabaseCharacter> databaseCharacters, char letter, int modifier, Consumer<DatabaseCharacter> onCreate) {
        return databaseCharacters.stream().filter(cha -> cha.getLetter() == letter && cha.getModifier() == modifier).findFirst().orElseGet(() -> {
            var databaseCharacter = new DatabaseCharacter(letter, modifier);
            onCreate.accept(databaseCharacter);
            return databaseCharacter;
        });
    }

}
