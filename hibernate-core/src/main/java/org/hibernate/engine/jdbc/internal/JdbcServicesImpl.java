package org.hibernate.engine.jdbc.internal;

import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.spi.ParameterMarkerStrategy;

import static java.util.Objects.requireNonNull;

/**
 * Standard implementation of the {@link JdbcServices} contract
 *
 * @author Steve Ebersole
 */
public class JdbcServicesImpl implements JdbcServices, ServiceRegistryAwareService, Configurable {
	private @Nullable ServiceRegistryImplementor serviceRegistry;
	private @Nullable JdbcEnvironment jdbcEnvironment;

	private @Nullable SqlStatementLogger sqlStatementLogger;
	private @Nullable ParameterMarkerStrategy parameterMarkerStrategy;

	// The SQL AST translators need these for every translation, and dialects
	// usually build them anew on every call. The Dialect contract requires
	// them to be immutable and stable for the lifetime of the dialect, so it
	// is enough to obtain them once; a racy initialization is harmless.
	private volatile @Nullable CteSupport cteSupport;
	private volatile @Nullable MutationSyntaxSupport mutationSyntaxSupport;

	public JdbcServicesImpl() {
	}

	public JdbcServicesImpl(@Nonnull ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void injectServices(@Nonnull ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(@Nonnull Map<String, Object> configValues) {
		requireNonNull( serviceRegistry,
				"ServiceRegistry has not been initialized" );
		this.jdbcEnvironment = serviceRegistry.requireService( JdbcEnvironment.class );
		this.sqlStatementLogger = serviceRegistry.requireService( SqlStatementLogger.class );
		this.parameterMarkerStrategy = serviceRegistry.requireService( ParameterMarkerStrategy.class );
	}

	@Override
	@Nonnull
	public JdbcEnvironment getJdbcEnvironment() {
		return requireNonNull( jdbcEnvironment,
				"JdbcEnvironment has not been initialized" );
	}

	@Override
	@Nonnull
	public JdbcConnectionAccess getBootstrapJdbcConnectionAccess() {
		return JdbcEnvironmentInitiator.buildJdbcConnectionAccess( serviceRegistry );
	}

	@Override
	@Nonnull
	public Dialect getDialect() {
		return getJdbcEnvironment().getDialect();
	}

	@Override
	@Nonnull
	public CteSupport getCteSupport() {
		CteSupport cteSupport = this.cteSupport;
		if ( cteSupport == null ) {
			cteSupport = getDialect().getCteSupport();
			this.cteSupport = cteSupport;
		}
		return cteSupport;
	}

	@Override
	@Nonnull
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		MutationSyntaxSupport mutationSyntaxSupport = this.mutationSyntaxSupport;
		if ( mutationSyntaxSupport == null ) {
			mutationSyntaxSupport = getDialect().getMutationSyntaxSupport();
			this.mutationSyntaxSupport = mutationSyntaxSupport;
		}
		return mutationSyntaxSupport;
	}

	@Override
	@Nonnull
	public SqlStatementLogger getSqlStatementLogger() {
		return requireNonNull( sqlStatementLogger,
				"SqlStatementLogger has not been initialized" );
	}

	@Override
	@Nonnull
	public ParameterMarkerStrategy getParameterMarkerStrategy() {
		return requireNonNull( parameterMarkerStrategy,
				"ParameterMarkerStrategy has not been initialized" );
	}

	@Override
	@Nonnull
	public SqlExceptionHelper getSqlExceptionHelper() {
		return getJdbcEnvironment().getSqlExceptionHelper();
	}

	@Override
	@Nonnull
	public JdbcMetadata getJdbcMetadata() {
		return getJdbcEnvironment().getJdbcMetadata();
	}

	@Deprecated(since = "8.0")
	@Override
	@Nonnull
	public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
		return getJdbcEnvironment().getJdbcMetadata()
				.getExtractedDatabaseMetaData();
	}

	@Override
	@Nonnull
	public LobCreator getLobCreator(@Nonnull LobCreationContext lobCreationContext) {
		return getJdbcEnvironment().getLobCreatorBuilder()
				.buildLobCreator( lobCreationContext );
	}

}
