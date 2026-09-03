/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.internal;

import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;

import static java.util.Objects.requireNonNull;

/**
 * Session-factory view of {@link JdbcServices} using the persistence unit's
 * {@link JdbcEnvironment}.
 */
@Internal
public final class PersistenceUnitJdbcServices implements JdbcServices {
	private final JdbcServices delegate;
	private final JdbcEnvironment jdbcEnvironment;

	public PersistenceUnitJdbcServices(JdbcServices delegate, JdbcEnvironment jdbcEnvironment) {
		this.delegate = requireNonNull( delegate );
		this.jdbcEnvironment = requireNonNull( jdbcEnvironment );
	}

	@Override
	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
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
	public ParameterMarkerStrategy getParameterMarkerStrategy() {
		return delegate.getParameterMarkerStrategy();
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
	public JdbcSelectExecutor getJdbcSelectExecutor() {
		return delegate.getJdbcSelectExecutor();
	}

	@Override
	public JdbcMutationExecutor getJdbcMutationExecutor() {
		return delegate.getJdbcMutationExecutor();
	}
}
