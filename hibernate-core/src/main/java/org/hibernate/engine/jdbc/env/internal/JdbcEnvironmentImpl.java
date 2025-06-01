/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.internal;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.LobCreatorBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.exception.internal.SQLExceptionTypeDelegate;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.internal.StandardSQLExceptionConverter;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.MappingSettings.DEFAULT_CATALOG;
import static org.hibernate.cfg.MappingSettings.DEFAULT_SCHEMA;
import static org.hibernate.engine.config.spi.StandardConverters.STRING;
import static org.hibernate.engine.jdbc.env.internal.LobCreatorBuilderImpl.makeLobCreatorBuilder;

/**
 * @author Steve Ebersole
 */
public class JdbcEnvironmentImpl implements JdbcEnvironment {
	private static final Logger log = Logger.getLogger( JdbcEnvironmentImpl.class );

	public static boolean isMultiTenancyEnabled(ServiceRegistry serviceRegistry) {
		return serviceRegistry.getService( MultiTenantConnectionProvider.class ) != null;
	}

	private final Dialect dialect;

	private final SqlAstTranslatorFactory sqlAstTranslatorFactory;

	private final SqlExceptionHelper sqlExceptionHelper;
	private final ExtractedDatabaseMetaData extractedMetaDataSupport;
	private final Identifier currentCatalog;
	private final Identifier currentSchema;
	private final IdentifierHelper identifierHelper;
	private final QualifiedObjectNameFormatter qualifiedObjectNameFormatter;
	private final LobCreatorBuilderImpl lobCreatorBuilder;

	private final NameQualifierSupport nameQualifierSupport;

	/**
	 * Constructor form used when the JDBC {@link DatabaseMetaData} is not available.
	 *
	 * @param serviceRegistry The service registry
	 * @param dialect The resolved dialect.
	 */
	public JdbcEnvironmentImpl(final ServiceRegistryImplementor serviceRegistry, final Dialect dialect) {
		this.dialect = dialect;

		sqlAstTranslatorFactory = resolveSqlAstTranslatorFactory( dialect );

		final ConfigurationService cfgService = configurationService( serviceRegistry );

		final NameQualifierSupport dialectNameQualifierSupport = dialect.getNameQualifierSupport();
		nameQualifierSupport =
				dialectNameQualifierSupport == null
						? NameQualifierSupport.BOTH  // assume both catalogs and schemas are supported
						: dialectNameQualifierSupport;

		sqlExceptionHelper =
				buildSqlExceptionHelper( dialect,
						logWarnings( cfgService, dialect ),
						logErrors( cfgService ) );

		final IdentifierHelperBuilder identifierHelperBuilder =
				identifierHelperBuilder( cfgService, nameQualifierSupport );

		final ExtractedDatabaseMetaDataImpl.Builder metaDataBuilder =
				new ExtractedDatabaseMetaDataImpl.Builder( this, false, null );

		identifierHelper = identifierHelper( dialect, identifierHelperBuilder, metaDataBuilder );

		extractedMetaDataSupport = metaDataBuilder.build();

		currentCatalog = identifierHelper.toIdentifier( cfgService.getSetting( DEFAULT_CATALOG, STRING ) );
		currentSchema = Identifier.toIdentifier( cfgService.getSetting( DEFAULT_SCHEMA, STRING ) );

		qualifiedObjectNameFormatter = new QualifiedObjectNameFormatterStandardImpl( nameQualifierSupport );

		lobCreatorBuilder = makeLobCreatorBuilder( dialect );
	}

	private static ConfigurationService configurationService(ServiceRegistryImplementor serviceRegistry) {
		return serviceRegistry.requireService( ConfigurationService.class );
	}

	private IdentifierHelperBuilder identifierHelperBuilder(
			ConfigurationService cfgService, NameQualifierSupport nameQualifierSupport) {
		final IdentifierHelperBuilder builder = IdentifierHelperBuilder.from( this );
		builder.setGloballyQuoteIdentifiers( globalQuoting( cfgService ) );
		builder.setSkipGlobalQuotingForColumnDefinitions( globalQuotingSkippedForColumnDefinitions( cfgService ) );
		builder.setAutoQuoteKeywords( autoKeywordQuoting( cfgService ) );
		builder.setNameQualifierSupport( nameQualifierSupport );
		return builder;
	}

	private static IdentifierHelper identifierHelper(
			Dialect dialect,
			IdentifierHelperBuilder builder,
			ExtractedDatabaseMetaDataImpl.Builder dbMetaDataBuilder) {
		try {
			final IdentifierHelper helper = dialect.buildIdentifierHelper( builder, null );
			dbMetaDataBuilder.setSupportsNamedParameters( dialect.supportsNamedParameters( null ) );
			if ( helper != null ) {
				return helper;
			}
		}
		catch (SQLException sqle) {
			// should never ever happen
			log.debug( "There was a problem accessing DatabaseMetaData in building the JdbcEnvironment", sqle );
		}
		return builder.build();
	}

	private static SqlAstTranslatorFactory resolveSqlAstTranslatorFactory(Dialect dialect) {
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = dialect.getSqlAstTranslatorFactory();
		return sqlAstTranslatorFactory == null ? new StandardSqlAstTranslatorFactory() : sqlAstTranslatorFactory;
	}

	private static boolean logWarnings(ConfigurationService cfgService, Dialect dialect) {
		return cfgService.getSetting(
				AvailableSettings.LOG_JDBC_WARNINGS,
				StandardConverters.BOOLEAN,
				dialect.isJdbcLogWarningsEnabledByDefault()
		);
	}

	private static boolean logErrors(ConfigurationService cfgService) {
		return cfgService.getSetting(
				AvailableSettings.LOG_JDBC_ERRORS,
				StandardConverters.BOOLEAN,
				true
		);
	}

	private static boolean globalQuoting(ConfigurationService cfgService) {
		return cfgService.getSetting(
				AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS,
				StandardConverters.BOOLEAN,
				false
		);
	}

	private boolean globalQuotingSkippedForColumnDefinitions(ConfigurationService cfgService) {
		return cfgService.getSetting(
				AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS_SKIP_COLUMN_DEFINITIONS,
				StandardConverters.BOOLEAN,
				false
		);
	}

	private static boolean autoKeywordQuoting(ConfigurationService cfgService) {
		return cfgService.getSetting(
				AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED,
				StandardConverters.BOOLEAN,
				false
		);
	}

	/**
	 * Constructor form used from testing
	 */
	public JdbcEnvironmentImpl(
			DatabaseMetaData databaseMetaData,
			Dialect dialect,
			JdbcConnectionAccess jdbcConnectionAccess) throws SQLException {
		this.dialect = dialect;

		sqlAstTranslatorFactory = resolveSqlAstTranslatorFactory( dialect );

		sqlExceptionHelper = buildSqlExceptionHelper( dialect, false, true );

		nameQualifierSupport = nameQualifierSupport( databaseMetaData, dialect );

		identifierHelper = identifierHelper( databaseMetaData, dialect );

		extractedMetaDataSupport =
				new ExtractedDatabaseMetaDataImpl.Builder( this, true, jdbcConnectionAccess )
						.apply( databaseMetaData )
						.setSupportsNamedParameters( databaseMetaData.supportsNamedParameters() )
						.build();

		currentCatalog = null;
		currentSchema = null;

		qualifiedObjectNameFormatter =
				new QualifiedObjectNameFormatterStandardImpl( nameQualifierSupport, databaseMetaData );

		lobCreatorBuilder = makeLobCreatorBuilder( dialect );
	}

	private IdentifierHelper identifierHelper(DatabaseMetaData databaseMetaData, Dialect dialect) {
		final IdentifierHelperBuilder identifierHelperBuilder = IdentifierHelperBuilder.from( this );
		identifierHelperBuilder.setNameQualifierSupport( nameQualifierSupport );
		try {
			final IdentifierHelper identifierHelper =
					dialect.buildIdentifierHelper( identifierHelperBuilder, databaseMetaData );
			if ( identifierHelper != null ) {
				return identifierHelper;
			}
		}
		catch (SQLException sqle) {
			// should never ever happen
			log.debug( "There was a problem accessing DatabaseMetaData in building the JdbcEnvironment", sqle );
		}
		return identifierHelperBuilder.build();
	}

	private NameQualifierSupport nameQualifierSupport(DatabaseMetaData databaseMetaData, Dialect dialect)
			throws SQLException {
		final NameQualifierSupport nameQualifierSupport = dialect.getNameQualifierSupport();
		return nameQualifierSupport == null ? determineNameQualifierSupport( databaseMetaData ) : nameQualifierSupport;
	}

	private NameQualifierSupport determineNameQualifierSupport(DatabaseMetaData databaseMetaData) throws SQLException {
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
			ServiceRegistryImplementor serviceRegistry,
			Dialect dialect,
			DatabaseMetaData databaseMetaData) {
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
			ServiceRegistryImplementor serviceRegistry,
			Dialect dialect,
			DatabaseMetaData databaseMetaData,
			JdbcConnectionAccess jdbcConnectionAccess) throws SQLException {
		this.dialect = dialect;

		sqlAstTranslatorFactory = resolveSqlAstTranslatorFactory( dialect );

		final ConfigurationService cfgService = configurationService( serviceRegistry );

		sqlExceptionHelper =
				buildSqlExceptionHelper( dialect,
						logWarnings( cfgService, dialect ),
						logErrors( cfgService ) );

		nameQualifierSupport = nameQualifierSupport( databaseMetaData, dialect );

		final IdentifierHelperBuilder identifierHelperBuilder =
				identifierHelperBuilder( cfgService, nameQualifierSupport );
		identifierHelper = identifierHelper( dialect, databaseMetaData, identifierHelperBuilder );

		extractedMetaDataSupport =
				new ExtractedDatabaseMetaDataImpl.Builder( this, true, jdbcConnectionAccess )
						.apply( databaseMetaData )
						.setConnectionSchemaName( determineCurrentSchemaName( databaseMetaData, serviceRegistry, dialect ) )
						.setSupportsNamedParameters( dialect.supportsNamedParameters( databaseMetaData ) )
						.build();

		// and that current-catalog and current-schema happen after it
		currentCatalog = identifierHelper.toIdentifier( extractedMetaDataSupport.getConnectionCatalogName() );
		currentSchema = identifierHelper.toIdentifier( extractedMetaDataSupport.getConnectionSchemaName() );

		qualifiedObjectNameFormatter =
				new QualifiedObjectNameFormatterStandardImpl( nameQualifierSupport, databaseMetaData );

		lobCreatorBuilder = makeLobCreatorBuilder( dialect, cfgService.getSettings(), databaseMetaData.getConnection() );
	}

	private static IdentifierHelper identifierHelper(
			Dialect dialect, DatabaseMetaData databaseMetaData, IdentifierHelperBuilder identifierHelperBuilder) {
		try {
			final IdentifierHelper identifierHelper =
					dialect.buildIdentifierHelper( identifierHelperBuilder, databaseMetaData );
			if ( identifierHelper != null ) {
				return identifierHelper;
			}
		}
		catch (SQLException sqle) {
			// should never ever happen
			log.debug( "There was a problem accessing DatabaseMetaData in building the JdbcEnvironment", sqle );
		}
		return identifierHelperBuilder.build();
	}

	public static final String SCHEMA_NAME_RESOLVER = "hibernate.schema_name_resolver";

	private String determineCurrentSchemaName(
			DatabaseMetaData databaseMetaData,
			ServiceRegistry serviceRegistry,
			Dialect dialect) {
		final SchemaNameResolver resolver = getSchemaNameResolver( serviceRegistry, dialect );
		try {
			return resolver.resolveSchemaName( databaseMetaData.getConnection(), dialect );
		}
		catch (Exception e) {
			log.debug( "Unable to resolve connection default schema", e );
			return null;
		}
	}

	private static SchemaNameResolver getSchemaNameResolver(ServiceRegistry serviceRegistry, Dialect dialect) {
		final Object setting =
				serviceRegistry.requireService( ConfigurationService.class )
						.getSettings().get( SCHEMA_NAME_RESOLVER );
		return setting == null
				? dialect.getSchemaNameResolver()
				: serviceRegistry.requireService( StrategySelector.class )
						.resolveDefaultableStrategy( SchemaNameResolver.class, setting,
								dialect.getSchemaNameResolver() );
	}

	private static SqlExceptionHelper buildSqlExceptionHelper(Dialect dialect, boolean logWarnings, boolean logErrors) {
		final SQLExceptionConversionDelegate dialectDelegate = dialect.buildSQLExceptionConversionDelegate();
		final SQLExceptionConversionDelegate[] delegates = dialectDelegate == null
				? new SQLExceptionConversionDelegate[] { new SQLExceptionTypeDelegate( dialect ), new SQLStateConversionDelegate( dialect ) }
				: new SQLExceptionConversionDelegate[] { dialectDelegate, new SQLExceptionTypeDelegate( dialect ), new SQLStateConversionDelegate( dialect ) };
		return new SqlExceptionHelper( new StandardSQLExceptionConverter( delegates ), logWarnings, logErrors );
	}

	@Override
	public Dialect getDialect() {
		return dialect;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return sqlAstTranslatorFactory;
	}

	@Override
	public ExtractedDatabaseMetaData getExtractedDatabaseMetaData() {
		return extractedMetaDataSupport;
	}

	@Override
	public Identifier getCurrentCatalog() {
		return currentCatalog;
	}

	@Override
	public Identifier getCurrentSchema() {
		return currentSchema;
	}

	@Override
	public QualifiedObjectNameFormatter getQualifiedObjectNameFormatter() {
		return qualifiedObjectNameFormatter;
	}

	@Override
	public IdentifierHelper getIdentifierHelper() {
		return identifierHelper;
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return nameQualifierSupport;
	}

	@Override
	public SqlExceptionHelper getSqlExceptionHelper() {
		return sqlExceptionHelper;
	}

	@Override
	public LobCreatorBuilder getLobCreatorBuilder() {
		return lobCreatorBuilder;
	}

}
