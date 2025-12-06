package com.zen.workflow.config;

import javax.sql.DataSource;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.sql.Connection;
import java.sql.SQLException;

@SuppressWarnings("rawtypes")
@Component
public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider {
    private static final Logger logger = LoggerFactory.getLogger(MultiTenantConnectionProviderImpl.class);

    private final DataSource datasource;

    public MultiTenantConnectionProviderImpl(DataSource dataSource) {
        this.datasource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return datasource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        logger.info("*** Workflow Service - Get connection for tenant: {}", tenantIdentifier);
        final Connection connection = getAnyConnection();
        try {
            connection.createStatement().execute("USE " + tenantIdentifier);
            logger.info("*** Workflow Service - Successfully switched to schema: {}", tenantIdentifier);
            return connection;
        } catch (SQLException e) {
            logger.error("*** Workflow Service - ERROR switching to schema {}: {}", tenantIdentifier, e.getMessage());
            connection.close();
            throw e;
        }
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        logger.info("Release connection for tenant {}", tenantIdentifier);
        String DEFAULT_TENANT = "common";
        connection.setSchema(DEFAULT_TENANT);
        releaseAnyConnection(connection);
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isUnwrappableAs(Class aClass) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> aClass) {
        return null;
    }
}
