package org.hibernate.tool;

import java.sql.Connection;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class EnversSchemaGenerator {
    private final SchemaExport export;

    public EnversSchemaGenerator(ServiceRegistry serviceRegistry, Configuration configuration) {
        configuration = configureAuditing(configuration);
        export = new SchemaExport(serviceRegistry, configuration);
    }

    public EnversSchemaGenerator(Configuration configuration) {
        configuration = configureAuditing(configuration);
        export = new SchemaExport(configuration);
    }

    public EnversSchemaGenerator(Configuration configuration, Properties properties) throws HibernateException {
        configuration = configureAuditing(configuration);
        export = new SchemaExport(configuration, properties);
    }

    public EnversSchemaGenerator(Configuration configuration, Connection connection) throws HibernateException {
        configuration = configureAuditing(configuration);
        export = new SchemaExport(configuration, connection);
    }

    public SchemaExport export() {
        return export;
    }

    private Configuration configureAuditing(Configuration configuration) {
        configuration.buildMappings();
		AuditConfiguration.getFor(configuration);
        return configuration;
    }
}
