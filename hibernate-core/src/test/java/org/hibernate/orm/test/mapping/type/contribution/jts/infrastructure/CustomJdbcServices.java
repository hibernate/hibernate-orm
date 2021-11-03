/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.contribution.jts.infrastructure;

import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;

/**
 * @author Steve Ebersole
 */
public class CustomJdbcServices implements JdbcServices, ServiceRegistryAwareService, Configurable {
	private final JdbcServices delegate;
	private final CustomJdbcSelectExecutor selectExecutor;

	public CustomJdbcServices(JdbcServices delegate) {
		this.delegate = delegate;
		selectExecutor = new CustomJdbcSelectExecutor();
	}

	@Override
	public JdbcSelectExecutor getJdbcSelectExecutor() {
		return selectExecutor;
	}

	@Override
	public JdbcEnvironment getJdbcEnvironment() {
		return delegate.getJdbcEnvironment();
	}

	@Override
	public JdbcConnectionAccess getBootstrapJdbcConnectionAccess() {
		return delegate.getBootstrapJdbcConnectionAccess();
	}

	@Override
	public Dialect getDialect() {
		return delegate.getDialect();
	}

	@Override
	public SqlStatementLogger getSqlStatementLogger() {
		return delegate.getSqlStatementLogger();
	}

	@Override
	public SqlExceptionHelper getSqlExceptionHelper() {
		return delegate.getSqlExceptionHelper();
	}

	@Override
	public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
		return delegate.getExtractedMetaDataSupport();
	}

	@Override
	public LobCreator getLobCreator(LobCreationContext lobCreationContext) {
		return delegate.getLobCreator( lobCreationContext );
	}

	@Override
	public JdbcMutationExecutor getJdbcMutationExecutor() {
		return delegate.getJdbcMutationExecutor();
	}

	@Override
	public void configure(Map configurationValues) {
		if ( delegate instanceof Configurable ) {
			( (Configurable) delegate ).configure( configurationValues );
		}
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		if ( delegate instanceof ServiceRegistryAwareService ) {
			( (ServiceRegistryAwareService) delegate ).injectServices( serviceRegistry );
		}
	}
}
