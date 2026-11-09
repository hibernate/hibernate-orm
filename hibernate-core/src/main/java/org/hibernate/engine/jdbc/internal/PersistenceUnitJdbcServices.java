package org.hibernate.engine.jdbc.internal;

import jakarta.annotation.Nonnull;
import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.sql.spi.ParameterMarkerStrategy;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;

import static java.util.Objects.requireNonNull;

/**
 * Session-factory view of {@link JdbcServices} using the persistence unit's
 * {@link JdbcEnvironment}.
 */
@Internal
public final class PersistenceUnitJdbcServices implements JdbcServices {
	private final @Nonnull JdbcServices delegate;
	private final @Nonnull JdbcEnvironment jdbcEnvironment;

	public PersistenceUnitJdbcServices(@Nonnull JdbcServices delegate, @Nonnull JdbcEnvironment jdbcEnvironment) {
		this.delegate = requireNonNull( delegate );
		this.jdbcEnvironment = requireNonNull( jdbcEnvironment );
	}

	@Override
	@Nonnull
	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	@Override
	@Nonnull
	public JdbcConnectionAccess getBootstrapJdbcConnectionAccess() {
		return delegate.getBootstrapJdbcConnectionAccess();
	}

	@Override
	@Nonnull
	public Dialect getDialect() {
		return delegate.getDialect();
	}

	@Override
	@Nonnull
	public JdbcMetadata getJdbcMetadata() {
		return jdbcEnvironment.getJdbcMetadata();
	}

	@Override
	@Nonnull
	public SqlStatementLogger getSqlStatementLogger() {
		return delegate.getSqlStatementLogger();
	}

	@Override
	@Nonnull
	public ParameterMarkerStrategy getParameterMarkerStrategy() {
		return delegate.getParameterMarkerStrategy();
	}

	@Override
	@Nonnull
	public SqlExceptionHelper getSqlExceptionHelper() {
		return delegate.getSqlExceptionHelper();
	}

	@Override
	@Nonnull
	public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
		return delegate.getExtractedMetaDataSupport();
	}

	@Override
	@Nonnull
	public LobCreator getLobCreator(@Nonnull LobCreationContext lobCreationContext) {
		return delegate.getLobCreator( lobCreationContext );
	}

	@Override
	@Nonnull
	public JdbcSelectExecutor getJdbcSelectExecutor() {
		return delegate.getJdbcSelectExecutor();
	}

	@Override
	@Nonnull
	public JdbcMutationExecutor getJdbcMutationExecutor() {
		return delegate.getJdbcMutationExecutor();
	}
}
