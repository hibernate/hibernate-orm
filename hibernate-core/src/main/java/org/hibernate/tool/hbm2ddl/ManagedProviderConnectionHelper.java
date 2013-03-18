/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.tool.hbm2ddl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * A {@link ConnectionHelper} implementation based on an internally
 * built and managed {@link ConnectionProvider}.
 *
 * @author Steve Ebersole
 */
class ManagedProviderConnectionHelper implements ConnectionHelper {
	private Properties cfgProperties;
	private StandardServiceRegistryImpl serviceRegistry;
	private Connection connection;

	public ManagedProviderConnectionHelper(Properties cfgProperties) {
		this.cfgProperties = cfgProperties;
	}

	public void prepare(boolean needsAutoCommit) throws SQLException {
		serviceRegistry = createServiceRegistry( cfgProperties );
		connection = serviceRegistry.getService( ConnectionProvider.class ).getConnection();
		if ( needsAutoCommit && ! connection.getAutoCommit() ) {
			connection.commit();
			connection.setAutoCommit( true );
		}
	}

	private static StandardServiceRegistryImpl createServiceRegistry(Properties properties) {
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );
		return (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().applySettings( properties ).build();
	}

	public Connection getConnection() throws SQLException {
		return connection;
	}

	public void release() throws SQLException {
		try {
			releaseConnection();
		}
		finally {
			releaseServiceRegistry();
		}
	}

	private void releaseConnection() throws SQLException {
		if ( connection != null ) {
			try {
				new SqlExceptionHelper().logAndClearWarnings( connection );
			}
			finally {
				try  {
					serviceRegistry.getService( ConnectionProvider.class ).closeConnection( connection );
				}
				finally {
					connection = null;
				}
			}
		}
	}

	private void releaseServiceRegistry() {
		if ( serviceRegistry != null ) {
			try {
				serviceRegistry.destroy();
			}
			finally {
				serviceRegistry = null;
			}
		}
	}
}
