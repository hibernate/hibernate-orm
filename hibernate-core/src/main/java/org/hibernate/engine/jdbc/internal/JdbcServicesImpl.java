/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
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

	private boolean multiTenancyEnabled;

	private SqlStatementLogger sqlStatementLogger;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map<String, Object> configValues) {
		this.jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		assert jdbcEnvironment != null : "JdbcEnvironment was not found!";

		this.multiTenancyEnabled = serviceRegistry.getService(MultiTenantConnectionProvider.class)!=null;

		final boolean showSQL = ConfigurationHelper.getBoolean( Environment.SHOW_SQL, configValues, false );
		final boolean formatSQL = ConfigurationHelper.getBoolean( Environment.FORMAT_SQL, configValues, false );
		final boolean highlightSQL = ConfigurationHelper.getBoolean( Environment.HIGHLIGHT_SQL, configValues, false );
		final long logSlowQuery = ConfigurationHelper.getLong( Environment.LOG_SLOW_QUERY, configValues, 0 );

		this.sqlStatementLogger = new SqlStatementLogger( showSQL, formatSQL, highlightSQL, logSlowQuery );
	}

	@Override
	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	@Override
	public JdbcConnectionAccess getBootstrapJdbcConnectionAccess() {
		return JdbcEnvironmentInitiator.buildBootstrapJdbcConnectionAccess( multiTenancyEnabled, serviceRegistry );
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

}
