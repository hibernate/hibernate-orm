/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.ResultSetWrapper;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
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
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( ConnectionProviderInitiator.class );
	
	private ServiceRegistryImplementor serviceRegistry;
	private JdbcEnvironment jdbcEnvironment;

	private MultiTenancyStrategy multiTenancyStrategy;

	private SqlStatementLogger sqlStatementLogger;

	private ResultSetWrapperImpl resultSetWrapper;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map configValues) {
		this.jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		assert jdbcEnvironment != null : "JdbcEnvironment was not found!";

		this.multiTenancyStrategy = MultiTenancyStrategy.determineMultiTenancyStrategy( configValues );

		final boolean showSQL = ConfigurationHelper.getBoolean( Environment.SHOW_SQL, configValues, false );
		final boolean formatSQL = ConfigurationHelper.getBoolean( Environment.FORMAT_SQL, configValues, false );
		final long logSlowQuery = ConfigurationHelper.getLong( Environment.LOG_SLOW_QUERY, configValues, 0 );

		final Object explicitSqlLogger = configValues.get( Environment.SQL_LOGGER );
		if ( explicitSqlLogger == null ) {
			// Default way of sqlStatementLogger initialization
			this.sqlStatementLogger = new SqlStatementLogger( showSQL, formatSQL, logSlowQuery );
		}
		else {
			final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );			
			// if we are explicitly supplied a SqlStatementLogger to use (in some form) -> use it..
			if ( explicitSqlLogger instanceof SqlStatementLogger ) {
				this.sqlStatementLogger = (SqlStatementLogger)explicitSqlLogger;
			}
			else if ( explicitSqlLogger instanceof Class ) {
				final Class loggerClass = (Class) explicitSqlLogger;
				LOG.instantiatingExplicitSQLLogger( loggerClass.getName() );
				this.sqlStatementLogger = instantiateExplicitSQLLogger(loggerClass);
			}
			else {
				String explicitSqlLoggerName = StringHelper.nullIfEmpty( explicitSqlLogger.toString() );
				if ( explicitSqlLoggerName != null ) {
					LOG.instantiatingExplicitSQLLogger( explicitSqlLoggerName );
					final Class loggerClass = strategySelector.selectStrategyImplementor(
							SqlStatementLogger.class,
							explicitSqlLoggerName
					);
					this.sqlStatementLogger = instantiateExplicitSQLLogger(loggerClass);
				}
				else {
					this.sqlStatementLogger = new SqlStatementLogger( showSQL, formatSQL, logSlowQuery );
				}
			}
		}

		resultSetWrapper = new ResultSetWrapperImpl( serviceRegistry );
	}

	@Override
	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
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
		return resultSetWrapper;
	}
	
	private SqlStatementLogger instantiateExplicitSQLLogger(Class loggerClass) {
		try {
			return (SqlStatementLogger) loggerClass.newInstance();
		}
		catch (Exception e) {
			throw new HibernateException( "Could not instantiate sql logger [" + loggerClass.getName() + "]", e );
		}
	}

}
