package edu.univ.erp.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * DataSourceProvider: provides two DataSources (erp and auth) using HikariCP.
 * It reads config from:
 *  - System property "config.file" (absolute path)
 *  - file "application.properties" at working dir
 *  - classpath resource "/application.properties"
 * Environment variables override properties if present:
 *  - DB_ERP_URL, DB_ERP_USER, DB_ERP_PASS
 *  - DB_AUTH_URL, DB_AUTH_USER, DB_AUTH_PASS
 */
public final class DataSourceProvider {

    private static final String CFG_FILE_PROP = "config.file";
    private static final Properties props = loadProperties();

    private static DataSource erpDs;
    private static DataSource authDs;

    private DataSourceProvider() {}

    private static Properties loadProperties() {
        Properties p = new Properties();
        try {
            System.out.println("DEBUG: DataSourceProvider starting loadProperties()");
            System.out.println("DEBUG: user.dir = " + System.getProperty("user.dir"));
            // 1) config file via -Dconfig.file
            String cfg = System.getProperty(CFG_FILE_PROP);
            if (cfg != null) {
                System.out.println("DEBUG: -Dconfig.file provided -> " + cfg);
                java.nio.file.Path cfgPath = Path.of(cfg);
                if (Files.exists(cfgPath)) {
                    try (InputStream in = new FileInputStream(cfgPath.toFile())) {
                        p.load(in);
                        System.out.println("DEBUG: Loaded properties from -Dconfig.file");
                        System.out.println("DEBUG: keys present: " + p.keySet());
                        return p;
                    }
                } else {
                    System.out.println("DEBUG: -Dconfig.file path does NOT exist: " + cfg);
                }
            } else {
                System.out.println("DEBUG: -Dconfig.file NOT provided");
            }

            // 2) application.properties in working dir
            Path local = Path.of(System.getProperty("user.dir")).resolve("application.properties");
            System.out.println("DEBUG: Looking for working-dir application.properties at: " + local.toAbsolutePath());
            if (Files.exists(local)) {
                try (InputStream in = new FileInputStream(local.toFile())) {
                    p.load(in);
                    System.out.println("DEBUG: Loaded properties from working dir: " + local.toAbsolutePath());
                    System.out.println("DEBUG: keys present: " + p.keySet());
                    return p;
                }
            } else {
                System.out.println("DEBUG: No application.properties in working dir");
            }

            // 3) classpath resource
            System.out.println("DEBUG: Looking for classpath /application.properties");
            try (InputStream in = DataSourceProvider.class.getResourceAsStream("/application.properties")) {
                if (in != null) {
                    p.load(in);
                    System.out.println("DEBUG: Loaded properties from classpath resource /application.properties");
                    System.out.println("DEBUG: keys present: " + p.keySet());
                    return p;
                } else {
                    System.out.println("DEBUG: classpath /application.properties not found");
                }
            }
        } catch (Exception ex) {
            System.err.println("DEBUG: exception while loading properties: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }

        // env override (unchanged)
        overrideFromEnv(p, "DB_ERP_URL", "db.erp.url");
        overrideFromEnv(p, "DB_ERP_USER", "db.erp.user");
        overrideFromEnv(p, "DB_ERP_PASS", "db.erp.password");
        overrideFromEnv(p, "DB_AUTH_URL", "db.auth.url");
        overrideFromEnv(p, "DB_AUTH_USER", "db.auth.user");
        overrideFromEnv(p, "DB_AUTH_PASS", "db.auth.password");

        System.out.println("DEBUG: After env overrides, keys present: " + p.keySet());
        // Show explicitly critical props we care about
        System.out.println("DEBUG: db.erp.url=" + p.getProperty("db.erp.url"));
        System.out.println("DEBUG: db.auth.url=" + p.getProperty("db.auth.url"));
        System.out.println("DEBUG: done loadProperties()");
        return p;
    }

    private static void overrideFromEnv(Properties p, String envKey, String propKey) {
        String v = System.getenv(envKey);
        if (v != null && !v.isEmpty()) p.setProperty(propKey, v);
    }

    public static synchronized DataSource erpDataSource() {
        if (erpDs == null) erpDs = createDs("db.erp.url", "db.erp.user", "db.erp.password");
        return erpDs;
    }

    public static synchronized DataSource authDataSource() {
        if (authDs == null) authDs = createDs("db.auth.url", "db.auth.user", "db.auth.password");
        return authDs;
    }

    private static DataSource createDs(String urlKey, String userKey, String passKey) {
        String url = props.getProperty(urlKey);
        String user = props.getProperty(userKey);
        String pass = props.getProperty(passKey);
        if (url == null) throw new IllegalStateException("Missing property: " + urlKey);
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        if (user != null) cfg.setUsername(user);
        if (pass != null) cfg.setPassword(pass);
        String maxPool = props.getProperty("db.pool.maxPoolSize");
        if (maxPool != null) cfg.setMaximumPoolSize(Integer.parseInt(maxPool));
        // optional: connection test query for MySQL
        cfg.setConnectionTestQuery("SELECT 1");
        cfg.setPoolName("erp-pool-" + Math.abs(url.hashCode()));
        return new HikariDataSource(cfg);
    }
}
