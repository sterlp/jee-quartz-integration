package org.sterl.ee.quartz.timer.quartz;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.quartz.utils.ConnectionProvider;

/**
 * https://github.com/sterlp/training/blob/master/jobs/src/main/java/org/sterl/education/jobs/SimpleConnectionProvider.java
 */
@AllArgsConstructor
public class SimpleConnectionProvider implements ConnectionProvider {
    @NonNull
    private final DataSource dataSource;

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void initialize() {
    }
}
