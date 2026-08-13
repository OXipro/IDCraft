package com.oxipro.idcraft.db.postgres;

public class PostgresConfig {

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    private final int poolSize;

    public PostgresConfig(String host, int port, String database, String user, String password, int poolSize) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.poolSize = poolSize;
    }

    public static PostgresConfig defaults(String host, String database, String user, String password) {
        return new PostgresConfig(host, 5432, database, user, password, 10);
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public int getPoolSize() {
        return poolSize;
    }

    String jdbcUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }
}
