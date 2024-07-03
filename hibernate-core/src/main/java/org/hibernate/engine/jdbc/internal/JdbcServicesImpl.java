/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * Standard implementation of the {@link JdbcServices} contract
 *
 * @author Steve Ebersole
 */
public class JdbcServicesImpl implements JdbcServices, ServiceRegistryAwareService, Configurable {
	private ServiceRegistryImplementor serviceRegistry;
	private JdbcEnvironment jdbcEnvironment;

	private SqlStatementLogger sqlStatementLogger;
	private ParameterMarkerStrategy parameterMarkerStrategy;

	public JdbcServicesImpl() {
	}

	public JdbcServicesImpl(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map<String, Object> configValues) {
		this.jdbcEnvironment = serviceRegistry.requireService( JdbcEnvironment.class );
		this.sqlStatementLogger = serviceRegistry.getService( SqlStatementLogger.class );
		this.parameterMarkerStrategy = serviceRegistry.getService( ParameterMarkerStrategy.class );
	}

	@Override
	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	@Override
	public JdbcConnectionAccess getBootstrapJdbcConnectionAccess() {
		return JdbcEnvironmentInitiator.buildBootstrapJdbcConnectionAccess( serviceRegistry );
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
	public ParameterMarkerStrategy getParameterMarkerStrategy() {
		return parameterMarkerStrategy;
	}

	@Override
	public SqlExceptionHelper getSqlExceptionHelper() {
		assert jdbcEnvironment != null : "JdbcEnvironment was not found";
		return jdbcEnvironment.getSqlExceptionHelper();
	}

	@Override
	public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
		assert jdbcEnvironment != null : "JdbcEnvironment was not found";
		return jdbcEnvironment.getExtractedDatabaseMetaData();
	}

	@Override
	public LobCreator getLobCreator(LobCreationContext lobCreationContext) {
		assert jdbcEnvironment != null : "JdbcEnvironment was not found";
		return jdbcEnvironment.getLobCreatorBuilder().buildLobCreator( lobCreationContext );
	}

}
