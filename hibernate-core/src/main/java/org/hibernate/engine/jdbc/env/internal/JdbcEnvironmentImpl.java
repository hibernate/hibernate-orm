package org.hibernate.engine.jdbc.env.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot;

import org.hibernate.dialect.lock.spi.TransactionConcurrency;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.TransactionSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.env.spi.LobCreatorBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.exception.internal.SQLExceptionTypeDelegate;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.internal.StandardSQLExceptionConverter;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;

import static org.hibernate.cfg.MappingSettings.DEFAULT_CATALOG;
import static org.hibernate.cfg.MappingSettings.DEFAULT_SCHEMA;
import static org.hibernate.engine.config.spi.StandardConverters.STRING;
import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_LOGGER;
import static org.hibernate.engine.jdbc.env.internal.LobCreatorBuilderImpl.makeLobCreatorBuilder;

/**
 * @author Steve Ebersole
 */
public class JdbcEnvironmentImpl implements JdbcEnvironment {

	private final @Nonnull Dialect dialect;

	private final @Nonnull SqlAstTranslatorFactory sqlAstTranslatorFactory;

	private final @Nonnull SqlExceptionHelper sqlExceptionHelper;
	private final @Nonnull ExtractedDatabaseMetaDataImpl extractedMetaDataSupport;
	private final @Nonnull JdbcMetadata jdbcMetadata;
	private final @Nonnull TransactionConcurrency transactionConcurrency;
	private final @Nullable Identifier currentCatalog;
	private final @Nullable Identifier currentSchema;
	private final @Nonnull IdentifierHelper identifierHelper;
	private final @Nonnull QualifiedObjectNameFormatter qualifiedObjectNameFormatter;
	private final @Nonnull LobCreatorBuilderImpl lobCreatorBuilder;

	private final @Nonnull NameQualifierSupport nameQualifierSupport;

	/**
	 * Constructor form used when the JDBC {@link DatabaseMetaData} is not available.
	 *
	 * @param serviceRegistry The service registry
	 * @param dialect The resolved dialect.
	 */
	public JdbcEnvironmentImpl(final @Nonnull ServiceRegistryImplementor serviceRegistry, final @Nonnull Dialect dialect) {
		this.dialect = dialect;

		sqlAstTranslatorFactory = resolveSqlAstTranslatorFactory( dialect );

		final var cfgService = configurationService( serviceRegistry );

		final var dialectNameQualifierSupport = dialect.getNameQualifierSupport();
		nameQualifierSupport =
				dialectNameQualifierSupport == null
						? NameQualifierSupport.BOTH  // assume both catalogs and schemas are supported
						: dialectNameQualifierSupport;

		sqlExceptionHelper =
				buildSqlExceptionHelper( dialect,
						logWarnings( cfgService, dialect ),
						logErrors( cfgService ) );

		extractedMetaDataSupport = new ExtractedDatabaseMetaDataImpl( this );
		jdbcMetadata = new JdbcMetadataImpl( extractedMetaDataSupport, dialect.getJdbcMetadataOverrides() );
		transactionConcurrency = resolveTransactionConcurrency(
				dialect, jdbcMetadata, null,
				cfgService.getSettings().get( TransactionSettings.TRANSACTION_CONCURRENCY ),
				JdbcMetadataOnBoot.DISALLOW );

		identifierHelper = identifierHelper(
				dialect,
				identifierHelperBuilder( cfgService, nameQualifierSupport )
		);

		currentCatalog = identifierHelper.toIdentifier( cfgService.getSetting( DEFAULT_CATALOG, STRING ) );
		currentSchema = Identifier.toIdentifier( cfgService.getSetting( DEFAULT_SCHEMA, STRING ) );

		qualifiedObjectNameFormatter =
				new QualifiedObjectNameFormatterStandardImpl( nameQualifierSupport, dialect.getCatalogSeparator() );

		lobCreatorBuilder = makeLobCreatorBuilder( dialect );

		logJdbcFetchSize( extractedMetaDataSupport.getDefaultFetchSize(), cfgService );
	}

	@Nonnull
	private static ConfigurationService configurationService(@Nonnull ServiceRegistryImplementor serviceRegistry) {
		return serviceRegistry.requireService( ConfigurationService.class );
	}

	@Nonnull
	private IdentifierHelperBuilder identifierHelperBuilder(
			@Nonnull ConfigurationService cfgService, @Nonnull NameQualifierSupport nameQualifierSupport) {
		final var builder = IdentifierHelperBuilder.from( this );
		builder.setGloballyQuoteIdentifiers( globalQuoting( cfgService ) );
		builder.setSkipGlobalQuotingForColumnDefinitions( globalQuotingSkippedForColumnDefinitions( cfgService ) );
		builder.setAutoQuoteKeywords( autoKeywordQuoting( cfgService ) );
		builder.setNameQualifierSupport( nameQualifierSupport );
		return builder;
	}

	@Nonnull
	private IdentifierHelper identifierHelper(@Nonnull Dialect dialect, @Nonnull IdentifierHelperBuilder builder) {
		return dialect.getIdentifierSupport().buildIdentifierHelper(
				new IdentifierHelperBuildRequest(
						builder,
						jdbcMetadata,
						dialect.getKeywordSupport(),
						nameQualifierSupport
				)
		);
	}

	@Nonnull
	private static SqlAstTranslatorFactory resolveSqlAstTranslatorFactory(@Nonnull Dialect dialect) {
		final var sqlAstTranslatorFactory = dialect.getSqlAstTranslatorFactory();
		return sqlAstTranslatorFactory == null ? new StandardSqlAstTranslatorFactory() : sqlAstTranslatorFactory;
	}

	private static boolean logWarnings(@Nonnull ConfigurationService cfgService, @Nonnull Dialect dialect) {
		return cfgService.getSetting(
				JdbcSettings.LOG_JDBC_WARNINGS,
				StandardConverters.BOOLEAN,
				dialect.isJdbcLogWarningsEnabledByDefault()
		);
	}

	private static boolean logErrors(@Nonnull ConfigurationService cfgService) {
		return cfgService.getSetting(
				JdbcSettings.LOG_JDBC_ERRORS,
				StandardConverters.BOOLEAN,
				true
		);
	}

	private static boolean globalQuoting(@Nonnull ConfigurationService cfgService) {
		return cfgService.getSetting(
				MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS,
				StandardConverters.BOOLEAN,
				false
		);
	}

	private static boolean globalQuotingSkippedForColumnDefinitions(@Nonnull ConfigurationService cfgService) {
		return cfgService.getSetting(
				MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS_SKIP_COLUMN_DEFINITIONS,
				StandardConverters.BOOLEAN,
				false
		);
	}

	private static boolean autoKeywordQuoting(@Nonnull ConfigurationService cfgService) {
		return cfgService.getSetting(
				MappingSettings.KEYWORD_AUTO_QUOTING_ENABLED,
				StandardConverters.BOOLEAN,
				false
		);
	}

	/**
	 * Constructor form used from testing
	 */
	public JdbcEnvironmentImpl(
			@Nonnull DatabaseMetaData databaseMetaData,
			@Nonnull Dialect dialect,
			@Nonnull JdbcConnectionAccess jdbcConnectionAccess) throws SQLException {
		this.dialect = dialect;

		sqlAstTranslatorFactory = resolveSqlAstTranslatorFactory( dialect );

		sqlExceptionHelper = buildSqlExceptionHelper( dialect, false, true );

		nameQualifierSupport = nameQualifierSupport( databaseMetaData, dialect );

		extractedMetaDataSupport =
				new ExtractedDatabaseMetaDataImpl( this, jdbcConnectionAccess, databaseMetaData );
		jdbcMetadata = new JdbcMetadataImpl( extractedMetaDataSupport, dialect.getJdbcMetadataOverrides() );
		transactionConcurrency = resolveTransactionConcurrency(
				dialect, jdbcMetadata, databaseMetaData.getConnection(), null,
				JdbcMetadataOnBoot.ALLOW );

		final var identifierHelperBuilder = IdentifierHelperBuilder.from( this );
		identifierHelperBuilder.setNameQualifierSupport( nameQualifierSupport );
		identifierHelper = identifierHelper( dialect, identifierHelperBuilder );

		currentCatalog = null;
		currentSchema = null;

		qualifiedObjectNameFormatter =
				new QualifiedObjectNameFormatterStandardImpl( nameQualifierSupport, databaseMetaData );

		lobCreatorBuilder = makeLobCreatorBuilder( dialect );
	}

	@Nonnull
	private NameQualifierSupport nameQualifierSupport(@Nonnull DatabaseMetaData databaseMetaData, @Nonnull Dialect dialect)
			throws SQLException {
		final var nameQualifierSupport = dialect.getNameQualifierSupport();
		return nameQualifierSupport == null ? determineNameQualifierSupport( databaseMetaData ) : nameQualifierSupport;
	}

	@Nonnull
	private NameQualifierSupport determineNameQualifierSupport(@Nonnull DatabaseMetaData databaseMetaData)
			throws SQLException {
		final boolean supportsCatalogs = databaseMetaData.supportsCatalogsInTableDefinitions();
		final boolean supportsSchemas = databaseMetaData.supportsSchemasInTableDefinitions();
		if ( supportsCatalogs && supportsSchemas ) {
			return NameQualifierSupport.BOTH;
		}
		else if ( supportsCatalogs ) {
			return NameQualifierSupport.CATALOG;
		}
		else if ( supportsSchemas ) {
			return NameQualifierSupport.SCHEMA;
		}
		else {
			return NameQualifierSupport.NONE;
		}
	}

	/**
	 * @deprecated currently used by Hibernate Reactive
	 * This version of the constructor should handle the case in which we do actually have
	 * the option to access the {@link DatabaseMetaData}, but since Hibernate Reactive is
	 * currently not making use of it we take a shortcut.
	 */
	@Deprecated
	public JdbcEnvironmentImpl(
			@Nonnull ServiceRegistryImplementor serviceRegistry,
			@Nonnull Dialect dialect,
			@Nullable DatabaseMetaData databaseMetaData) {
		this( serviceRegistry, dialect );
	}

	/**
	 * The main constructor form.
	 * Builds a {@code JdbcEnvironment} using the available {@link DatabaseMetaData}.
	 *
	 * @param serviceRegistry The service registry
	 * @param dialect The resolved dialect
	 * @param databaseMetaData The available DatabaseMetaData
	 *
	 */
	public JdbcEnvironmentImpl(
			@Nonnull ServiceRegistryImplementor serviceRegistry,
			@Nonnull Dialect dialect,
			@Nonnull DatabaseMetaData databaseMetaData,
			@Nonnull JdbcConnectionAccess jdbcConnectionAccess) throws SQLException {
		this.dialect = dialect;

		sqlAstTranslatorFactory = resolveSqlAstTranslatorFactory( dialect );

		final var cfgService = configurationService( serviceRegistry );

		sqlExceptionHelper =
				buildSqlExceptionHelper( dialect,
						logWarnings( cfgService, dialect ),
						logErrors( cfgService ) );

		nameQualifierSupport = nameQualifierSupport( databaseMetaData, dialect );

		extractedMetaDataSupport =
				new ExtractedDatabaseMetaDataImpl( this, jdbcConnectionAccess, databaseMetaData );
		jdbcMetadata = new JdbcMetadataImpl( extractedMetaDataSupport, dialect.getJdbcMetadataOverrides() );
		transactionConcurrency = resolveTransactionConcurrency(
				dialect, jdbcMetadata, databaseMetaData.getConnection(),
				cfgService.getSettings().get( TransactionSettings.TRANSACTION_CONCURRENCY ),
				JdbcEnvironmentInitiator.jdbcMetadataAccess( cfgService.getSettings() ) );

		identifierHelper = identifierHelper(
				dialect,
				identifierHelperBuilder( cfgService, nameQualifierSupport )
		);

		// and that current-catalog and current-schema happen after it
		currentCatalog = identifierHelper.toIdentifier( extractedMetaDataSupport.getConnectionCatalogName() );
		currentSchema = identifierHelper.toIdentifier( extractedMetaDataSupport.getConnectionSchemaName() );

		qualifiedObjectNameFormatter =
				new QualifiedObjectNameFormatterStandardImpl( nameQualifierSupport, databaseMetaData );

		lobCreatorBuilder = makeLobCreatorBuilder( dialect, cfgService.getSettings(), databaseMetaData.getConnection() );

		logJdbcFetchSize( extractedMetaDataSupport.getDefaultFetchSize(), cfgService );
	}

	@Nonnull
	private static SqlExceptionHelper buildSqlExceptionHelper(
			@Nonnull Dialect dialect,
			boolean logWarnings,
			boolean logErrors) {
		final var dialectDelegate = dialect.buildSQLExceptionConversionDelegate();
		final var delegates = dialectDelegate == null
				? new SQLExceptionConversionDelegate[] { new SQLExceptionTypeDelegate( dialect ), new SQLStateConversionDelegate( dialect ) }
				: new SQLExceptionConversionDelegate[] { dialectDelegate, new SQLExceptionTypeDelegate( dialect ), new SQLStateConversionDelegate( dialect ) };
		return new SqlExceptionHelper( new StandardSQLExceptionConverter( delegates ), logWarnings, logErrors );
	}

	@Override
	@Nonnull
	public Dialect getDialect() {
		return dialect;
	}

	@Override
	@Nonnull
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return sqlAstTranslatorFactory;
	}

	@Override
	@Nonnull
	public TransactionConcurrency getTransactionConcurrency() {
		return transactionConcurrency;
	}

	@Override
	@Nonnull
	public JdbcMetadata getJdbcMetadata() {
		return jdbcMetadata;
	}

	@Deprecated(since = "8.0")
	@Override
	@Nonnull
	public ExtractedDatabaseMetaData getExtractedDatabaseMetaData() {
		return extractedMetaDataSupport;
	}

	@Override
	@Nullable
	public Identifier getCurrentCatalog() {
		return currentCatalog;
	}

	@Override
	@Nullable
	public Identifier getCurrentSchema() {
		return currentSchema;
	}

	@Override
	@Nonnull
	public QualifiedObjectNameFormatter getQualifiedObjectNameFormatter() {
		return qualifiedObjectNameFormatter;
	}

	@Override
	@Nonnull
	public IdentifierHelper getIdentifierHelper() {
		return identifierHelper;
	}

	@Override
	@Nonnull
	public NameQualifierSupport getNameQualifierSupport() {
		return nameQualifierSupport;
	}

	@Override
	@Nonnull
	public SqlExceptionHelper getSqlExceptionHelper() {
		return sqlExceptionHelper;
	}

	@Override
	@Nonnull
	public LobCreatorBuilder getLobCreatorBuilder() {
		return lobCreatorBuilder;
	}

	private static void logJdbcFetchSize(int defaultFetchSize, @Nonnull ConfigurationService cfgService) {
		if ( !cfgService.getSettings().containsKey( JdbcSettings.STATEMENT_FETCH_SIZE ) ) {
			if ( defaultFetchSize > 0 && defaultFetchSize < 100 ) {
				JDBC_LOGGER.warnLowFetchSize( defaultFetchSize );
			}
			else {
				JDBC_LOGGER.usingFetchSize( defaultFetchSize );
			}
		}
	}
	@Nonnull
	private static TransactionConcurrency resolveTransactionConcurrency(
			@Nonnull Dialect dialect, @Nonnull JdbcMetadata metadata, @Nullable Connection connection,
			@Nullable Object declaration, @Nonnull JdbcMetadataOnBoot access) {
		if ( declaration instanceof TransactionConcurrency supplied ) {
			return supplied;
		}
		return dialect.getLockingSupport().getTransactionConcurrencyResolver()
				.resolve( dialect, metadata, connection, declaration, access );
	}

}
