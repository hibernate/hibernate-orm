/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.tools.hbm2ddl;

import java.sql.Connection;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class EnversSchemaGenerator {
	private final SchemaExport export;

	public EnversSchemaGenerator(ServiceRegistry serviceRegistry, Configuration configuration) {
		configuration = configureAuditing( configuration );
		export = new SchemaExport( serviceRegistry, configuration );
	}

	public EnversSchemaGenerator(Configuration configuration) {
		configuration = configureAuditing( configuration );
		export = new SchemaExport( configuration );
	}

	public EnversSchemaGenerator(Configuration configuration, Properties properties) throws HibernateException {
		configuration = configureAuditing( configuration );
		export = new SchemaExport( configuration, properties );
	}

	public EnversSchemaGenerator(Configuration configuration, Connection connection) throws HibernateException {
		configuration = configureAuditing( configuration );
		export = new SchemaExport( configuration, connection );
	}

	public SchemaExport export() {
		return export;
	}

	private Configuration configureAuditing(Configuration configuration) {
		configuration.buildMappings();
		AuditConfiguration.getFor( configuration );
		return configuration;
	}
}
