package com.oxipro.idcraft.db.mysql;

public class MySqlConfig {

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    private final int poolSize;

    public MySqlConfig(String host, int port, String database, String user, String password, int poolSize) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.poolSize = poolSize;
    }

    public static MySqlConfig defaults(String host, String database, String user, String password) {
        return new MySqlConfig(host, 3306, database, user, password, 10);
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
        return "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC";
    }
}
