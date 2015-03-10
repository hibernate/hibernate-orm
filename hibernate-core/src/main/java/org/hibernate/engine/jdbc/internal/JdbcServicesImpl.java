/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.internal;

import java.util.Map;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.ResultSetWrapper;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard implementation of the {@link JdbcServices} contract
 *
 * @author Steve Ebersole
 */
public class JdbcServicesImpl implements JdbcServices, ServiceRegistryAwareService, Configurable {
	private ServiceRegistryImplementor serviceRegistry;
	private JdbcEnvironment jdbcEnvironment;

	private MultiTenancyStrategy multiTenancyStrategy;

	private ConnectionProvider connectionProvider;
	private SqlStatementLogger sqlStatementLogger;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map configValues) {
		this.jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		assert jdbcEnvironment != null : "JdbcEnvironment was not found!";

		this.multiTenancyStrategy = MultiTenancyStrategy.determineMultiTenancyStrategy( configValues );
		this.connectionProvider = MultiTenancyStrategy.NONE == multiTenancyStrategy ?
				serviceRegistry.getService( ConnectionProvider.class ) :
				null;

		final boolean showSQL = ConfigurationHelper.getBoolean( Environment.SHOW_SQL, configValues, false );
		final boolean formatSQL = ConfigurationHelper.getBoolean( Environment.FORMAT_SQL, configValues, false );

		this.sqlStatementLogger =  new SqlStatementLogger( showSQL, formatSQL );
	}

	@Override
	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	@Override
	public ConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}

	@Override
	public JdbcConnectionAccess getBootstrapJdbcConnectionAccess() {
		return JdbcEnvironmentInitiator.buildBootstrapJdbcConnectionAccess( multiTenancyStrategy, serviceRegistry );
	}

	@Override
	public Dialect getDialect() {
		if ( jdbcEnvironment != null ) {
			return jdbcEnvironment.getDialect();
		}
		return null;
	}

	@Override
	public SqlStatementLogger getSqlStatementLogger() {
		return sqlStatementLogger;
	}

	@Override
	public SqlExceptionHelper getSqlExceptionHelper() {
		if ( jdbcEnvironment != null ) {
			return jdbcEnvironment.getSqlExceptionHelper();
		}
		return null;
	}

	@Override
	public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
		if ( jdbcEnvironment != null ) {
			return jdbcEnvironment.getExtractedDatabaseMetaData();
		}
		return null;
	}

	@Override
	public LobCreator getLobCreator(LobCreationContext lobCreationContext) {
		if ( jdbcEnvironment != null ) {
			return jdbcEnvironment.getLobCreatorBuilder().buildLobCreator( lobCreationContext );
		}
		return null;
	}

	@Override
	public ResultSetWrapper getResultSetWrapper() {
		return ResultSetWrapperImpl.INSTANCE;
	}
}
