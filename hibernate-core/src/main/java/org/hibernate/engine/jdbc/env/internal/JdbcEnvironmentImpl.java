/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

		this.sqlAstTranslatorFactory = resolveSqlAstTranslatorFactory( dialect );

		final ConfigurationService cfgService = serviceRegistry.requireService( ConfigurationService.class );

		NameQualifierSupport nameQualifierSupport = dialect.getNameQualifierSupport();
		if ( nameQualifierSupport == null ) {
			// assume both catalogs and schemas are supported
			nameQualifierSupport = NameQualifierSupport.BOTH;
		}
		this.nameQualifierSupport = nameQualifierSupport;

		this.sqlExceptionHelper = buildSqlExceptionHelper( dialect, logWarnings( cfgService, dialect ) );

		final IdentifierHelperBuilder identifierHelperBuilder = IdentifierHelperBuilder.from( this );
		identifierHelperBuilder.setGloballyQuoteIdentifiers( globalQuoting( cfgService ) );
		identifierHelperBuilder.setSkipGlobalQuotingForColumnDefinitions( globalQuotingSkippedForColumnDefinitions(
				cfgService ) );
		identifierHelperBuilder.setAutoQuoteKeywords( autoKeywordQuoting( cfgService ) );
		identifierHelperBuilder.setNameQualifierSupport( nameQualifierSupport );

		IdentifierHelper identifierHelper = null;
		ExtractedDatabaseMetaDataImpl.Builder dbMetaDataBuilder = new ExtractedDatabaseMetaDataImpl.Builder( this, false, null );
		try {
			identifierHelper = dialect.buildIdentifierHelper( identifierHelperBuilder, null );
			dbMetaDataBuilder.setSupportsNamedParameters( dialect.supportsNamedParameters( null ) );
		}
		catch (SQLException sqle) {
			// should never ever happen
			log.debug( "There was a problem accessing DatabaseMetaData in building the JdbcEnvironment", sqle );
		}
		if ( identifierHelper == null ) {
			identifierHelper = identifierHelperBuilder.build();
		}
		this.identifierHelper = identifierHelper;

		this.extractedMetaDataSupport = dbMetaDataBuilder.build();

		this.currentCatalog = identifierHelper.toIdentifier(
				cfgService.getSetting( AvailableSettings.DEFAULT_CATALOG, StandardConverters.STRING )
		);
		this.currentSchema = Identifier.toIdentifier(
				cfgService.getSetting( AvailableSettings.DEFAULT_SCHEMA, StandardConverters.STRING )
		);

		this.qualifiedObjectNameFormatter = new QualifiedObjectNameFormatterStandardImpl( nameQualifierSupport );

		this.lobCreatorBuilder = LobCreatorBuilderImpl.makeLobCreatorBuilder();
	}

	private static SqlAstTranslatorFactory resolveSqlAstTranslatorFactory(Dialect dialect) {
		if ( dialect.getSqlAstTranslatorFactory() != null ) {
			return dialect.getSqlAstTranslatorFactory();
		}

		return new StandardSqlAstTranslatorFactory();
	}

	private static boolean logWarnings(ConfigurationService cfgService, Dialect dialect) {
		return cfgService.getSetting(
				AvailableSettings.LOG_JDBC_WARNINGS,
				StandardConverters.BOOLEAN,
				dialect.isJdbcLogWarningsEnabledByDefault()
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

		this.sqlAstTranslatorFactory = resolveSqlAstTranslatorFactory( dialect );

		this.sqlExceptionHelper = buildSqlExceptionHelper( dialect, false );

		NameQualifierSupport nameQualifierSupport = dialect.getNameQualifierSupport();
		if ( nameQualifierSupport == null ) {
			nameQualifierSupport = determineNameQualifierSupport( databaseMetaData );
		}
		this.nameQualifierSupport = nameQualifierSupport;

		final IdentifierHelperBuilder identifierHelperBuilder = IdentifierHelperBuilder.from( this );
		identifierHelperBuilder.setNameQualifierSupport( nameQualifierSupport );
		IdentifierHelper identifierHelper = null;
		try {
			identifierHelper = dialect.buildIdentifierHelper( identifierHelperBuilder, databaseMetaData );
		}
		catch (SQLException sqle) {
			// should never ever happen
			log.debug( "There was a problem accessing DatabaseMetaData in building the JdbcEnvironment", sqle );
		}
		if ( identifierHelper == null ) {
			identifierHelper = identifierHelperBuilder.build();
		}
		this.identifierHelper = identifierHelper;

		this.extractedMetaDataSupport = new ExtractedDatabaseMetaDataImpl.Builder( this, true, jdbcConnectionAccess )
				.apply( databaseMetaData )
				.setSupportsNamedParameters( databaseMetaData.supportsNamedParameters() )
				.build();

		this.currentCatalog = null;
		this.currentSchema = null;

		this.qualifiedObjectNameFormatter = new QualifiedObjectNameFormatterStandardImpl(
				nameQualifierSupport,
				databaseMetaData
		);

		this.lobCreatorBuilder = LobCreatorBuilderImpl.makeLobCreatorBuilder();
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
	 * the option to access the DatabaseMetaData, but since Hibernate Reactive is currently
	 * not making use of it we take a shortcut.
	 */
	@Deprecated
	public JdbcEnvironmentImpl(
			ServiceRegistryImplementor serviceRegistry,
			Dialect dialect,
			DatabaseMetaData databaseMetaData
			/*JdbcConnectionAccess jdbcConnectionAccess*/) throws SQLException {
		this(serviceRegistry, dialect);
	}

	/**
	 * The main constructor form.  Builds a JdbcEnvironment using the available DatabaseMetaData
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

		this.sqlAstTranslatorFactory = resolveSqlAstTranslatorFactory( dialect );

		final ConfigurationService cfgService = serviceRegistry.requireService( ConfigurationService.class );

		this.sqlExceptionHelper = buildSqlExceptionHelper( dialect, logWarnings( cfgService, dialect ) );

		NameQualifierSupport nameQualifierSupport = dialect.getNameQualifierSupport();
		if ( nameQualifierSupport == null ) {
			nameQualifierSupport = determineNameQualifierSupport( databaseMetaData );
		}
		this.nameQualifierSupport = nameQualifierSupport;

		final IdentifierHelperBuilder identifierHelperBuilder = IdentifierHelperBuilder.from( this );
		identifierHelperBuilder.setGloballyQuoteIdentifiers( globalQuoting( cfgService ) );
		identifierHelperBuilder.setSkipGlobalQuotingForColumnDefinitions( globalQuotingSkippedForColumnDefinitions(
				cfgService ) );
		identifierHelperBuilder.setAutoQuoteKeywords( autoKeywordQuoting( cfgService ) );
		identifierHelperBuilder.setNameQualifierSupport( nameQualifierSupport );
		IdentifierHelper identifierHelper = null;
		try {
			identifierHelper = dialect.buildIdentifierHelper( identifierHelperBuilder, databaseMetaData );
		}
		catch (SQLException sqle) {
			// should never ever happen
			log.debug( "There was a problem accessing DatabaseMetaData in building the JdbcEnvironment", sqle );
		}
		if ( identifierHelper == null ) {
			identifierHelper = identifierHelperBuilder.build();
		}
		this.identifierHelper = identifierHelper;

		this.extractedMetaDataSupport = new ExtractedDatabaseMetaDataImpl.Builder( this, true, jdbcConnectionAccess )
				.apply( databaseMetaData )
				.setConnectionSchemaName( determineCurrentSchemaName( databaseMetaData, serviceRegistry, dialect ) )
				.setSupportsNamedParameters( dialect.supportsNamedParameters( databaseMetaData ) )
				.build();

		// and that current-catalog and current-schema happen after it
		this.currentCatalog = identifierHelper.toIdentifier( extractedMetaDataSupport.getConnectionCatalogName() );
		this.currentSchema = identifierHelper.toIdentifier( extractedMetaDataSupport.getConnectionSchemaName() );

		this.qualifiedObjectNameFormatter = new QualifiedObjectNameFormatterStandardImpl(
				nameQualifierSupport,
				databaseMetaData
		);

		this.lobCreatorBuilder = LobCreatorBuilderImpl.makeLobCreatorBuilder(
				dialect,
				cfgService.getSettings(),
				databaseMetaData.getConnection()
		);
	}

	public static final String SCHEMA_NAME_RESOLVER = "hibernate.schema_name_resolver";

	private String determineCurrentSchemaName(
			DatabaseMetaData databaseMetaData,
			ServiceRegistry serviceRegistry,
			Dialect dialect) {
		final SchemaNameResolver schemaNameResolver;

		final Object setting = serviceRegistry.requireService( ConfigurationService.class ).getSettings().get(
				SCHEMA_NAME_RESOLVER );
		if ( setting == null ) {
			schemaNameResolver = dialect.getSchemaNameResolver();
		}
		else {
			schemaNameResolver = serviceRegistry.requireService( StrategySelector.class ).resolveDefaultableStrategy(
					SchemaNameResolver.class,
					setting,
					dialect.getSchemaNameResolver()
			);
		}

		try {
			return schemaNameResolver.resolveSchemaName( databaseMetaData.getConnection(), dialect );
		}
		catch (Exception e) {
			log.debug( "Unable to resolve connection default schema", e );
			return null;
		}
	}

	private SqlExceptionHelper buildSqlExceptionHelper(Dialect dialect, boolean logWarnings) {
		SQLExceptionConversionDelegate dialectDelegate = dialect.buildSQLExceptionConversionDelegate();
		SQLExceptionConversionDelegate[] delegates = dialectDelegate == null
				? new SQLExceptionConversionDelegate[] { new SQLExceptionTypeDelegate( dialect ), new SQLStateConversionDelegate( dialect ) }
				: new SQLExceptionConversionDelegate[] { dialectDelegate, new SQLExceptionTypeDelegate( dialect ), new SQLStateConversionDelegate( dialect ) };
		return new SqlExceptionHelper( new StandardSQLExceptionConverter(delegates), logWarnings );
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
