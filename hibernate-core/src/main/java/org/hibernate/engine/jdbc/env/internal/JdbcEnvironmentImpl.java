/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.internal;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.LobCreatorBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.TypeInfo;
import org.hibernate.exception.internal.SQLExceptionTypeDelegate;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.internal.StandardSQLExceptionConverter;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class JdbcEnvironmentImpl implements JdbcEnvironment {
	private static final Logger log = Logger.getLogger( JdbcEnvironmentImpl.class );

	private final Dialect dialect;

	private final SqlExceptionHelper sqlExceptionHelper;
	private final ExtractedDatabaseMetaData extractedMetaDataSupport;
	private final Identifier currentCatalog;
	private final Identifier currentSchema;
	private final IdentifierHelper identifierHelper;
	private final QualifiedObjectNameFormatter qualifiedObjectNameFormatter;
	private final LobCreatorBuilderImpl lobCreatorBuilder;

	private final LinkedHashSet<TypeInfo> typeInfoSet = new LinkedHashSet<TypeInfo>();
	private final NameQualifierSupport nameQualifierSupport;

	/**
	 * Constructor form used when the JDBC {@link java.sql.DatabaseMetaData} is not available.
	 *
	 * @param serviceRegistry The service registry
	 * @param dialect The resolved dialect.
	 */
	public JdbcEnvironmentImpl(ServiceRegistryImplementor serviceRegistry, Dialect dialect) {
		this.dialect = dialect;

		final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );

		NameQualifierSupport nameQualifierSupport = dialect.getNameQualifierSupport();
		if ( nameQualifierSupport == null ) {
			// assume both catalogs and schemas are supported
			nameQualifierSupport = NameQualifierSupport.BOTH;
		}
		this.nameQualifierSupport = nameQualifierSupport;

		this.sqlExceptionHelper = buildSqlExceptionHelper( dialect, logWarnings( cfgService, dialect ) );

		final IdentifierHelperBuilder identifierHelperBuilder = IdentifierHelperBuilder.from( this );
		identifierHelperBuilder.setGloballyQuoteIdentifiers( globalQuoting( cfgService ) );
		identifierHelperBuilder.setSkipGlobalQuotingForColumnDefinitions( globalQuotingSkippedForColumnDefinitions( cfgService ) );
		identifierHelperBuilder.setAutoQuoteKeywords( autoKeywordQuoting( cfgService ) );
		identifierHelperBuilder.setNameQualifierSupport( nameQualifierSupport );

		IdentifierHelper identifierHelper = null;
		ExtractedDatabaseMetaDataImpl.Builder dbMetaDataBuilder = new ExtractedDatabaseMetaDataImpl.Builder( this );
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
	 *
	 * @param dialect The dialect
	 */
	public JdbcEnvironmentImpl(DatabaseMetaData databaseMetaData, Dialect dialect) throws SQLException {
		this.dialect = dialect;

		this.sqlExceptionHelper = buildSqlExceptionHelper( dialect, false );

		this.extractedMetaDataSupport = new ExtractedDatabaseMetaDataImpl.Builder( this )
				.apply( databaseMetaData )
				.setSupportsNamedParameters( databaseMetaData.supportsNamedParameters() )
				.build();

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
	 * The main constructor form.  Builds a JdbcEnvironment using the available DatabaseMetaData
	 *
	 * @param serviceRegistry The service registry
	 * @param dialect The resolved dialect
	 * @param databaseMetaData The available DatabaseMetaData
	 *
	 * @throws SQLException
	 */
	public JdbcEnvironmentImpl(
			ServiceRegistryImplementor serviceRegistry,
			Dialect dialect,
			DatabaseMetaData databaseMetaData) throws SQLException {
		this.dialect = dialect;

		final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );

		this.sqlExceptionHelper = buildSqlExceptionHelper( dialect, logWarnings( cfgService, dialect ) );

		this.extractedMetaDataSupport = new ExtractedDatabaseMetaDataImpl.Builder( this )
				.apply( databaseMetaData )
				.setConnectionSchemaName( determineCurrentSchemaName( databaseMetaData, serviceRegistry, dialect ) )
				.setSupportsNamedParameters(dialect.supportsNamedParameters(databaseMetaData))
				.build();

		NameQualifierSupport nameQualifierSupport = dialect.getNameQualifierSupport();
		if ( nameQualifierSupport == null ) {
			nameQualifierSupport = determineNameQualifierSupport( databaseMetaData );
		}
		this.nameQualifierSupport = nameQualifierSupport;

		final IdentifierHelperBuilder identifierHelperBuilder = IdentifierHelperBuilder.from( this );
		identifierHelperBuilder.setGloballyQuoteIdentifiers( globalQuoting( cfgService ) );
		identifierHelperBuilder.setSkipGlobalQuotingForColumnDefinitions( globalQuotingSkippedForColumnDefinitions( cfgService ) );
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

		// and that current-catalog and current-schema happen afterQuery it
		this.currentCatalog = identifierHelper.toIdentifier( extractedMetaDataSupport.getConnectionCatalogName() );
		this.currentSchema = identifierHelper.toIdentifier( extractedMetaDataSupport.getConnectionSchemaName() );

		this.qualifiedObjectNameFormatter = new QualifiedObjectNameFormatterStandardImpl(
				nameQualifierSupport,
				databaseMetaData
		);

		this.typeInfoSet.addAll( TypeInfo.extractTypeInfo( databaseMetaData ) );

		this.lobCreatorBuilder = LobCreatorBuilderImpl.makeLobCreatorBuilder(
				cfgService.getSettings(),
				databaseMetaData.getConnection()
		);
	}

	public static final String SCHEMA_NAME_RESOLVER = "hibernate.schema_name_resolver";

	private String determineCurrentSchemaName(
			DatabaseMetaData databaseMetaData,
			ServiceRegistry serviceRegistry,
			Dialect dialect) throws SQLException {
		final SchemaNameResolver schemaNameResolver;

		final Object setting = serviceRegistry.getService( ConfigurationService.class ).getSettings().get( SCHEMA_NAME_RESOLVER );
		if ( setting == null ) {
			schemaNameResolver = dialect.getSchemaNameResolver();
		}
		else {
			schemaNameResolver = serviceRegistry.getService( StrategySelector.class ).resolveDefaultableStrategy(
					SchemaNameResolver.class,
					setting,
					dialect.getSchemaNameResolver()
			);
		}

		try {
			return schemaNameResolver.resolveSchemaName( databaseMetaData.getConnection(), dialect );
		}
		catch (Exception e) {
			// for now, just ignore the exception.
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	private SqlExceptionHelper buildSqlExceptionHelper(Dialect dialect, boolean logWarnings) {
		final StandardSQLExceptionConverter sqlExceptionConverter = new StandardSQLExceptionConverter();
		sqlExceptionConverter.addDelegate( dialect.buildSQLExceptionConversionDelegate() );
		sqlExceptionConverter.addDelegate( new SQLExceptionTypeDelegate( dialect ) );
		// todo : vary this based on extractedMetaDataSupport.getSqlStateType()
		sqlExceptionConverter.addDelegate( new SQLStateConversionDelegate( dialect ) );
		return new SqlExceptionHelper( sqlExceptionConverter, logWarnings );
	}

	private Set<String> buildMergedReservedWords(Dialect dialect, DatabaseMetaData dbmd) throws SQLException {
		Set<String> reservedWords = new HashSet<String>();
		reservedWords.addAll( dialect.getKeywords() );
		// todo : do we need to explicitly handle SQL:2003 keywords?
		reservedWords.addAll( Arrays.asList( dbmd.getSQLKeywords().split( "," ) ) );
		return reservedWords;
	}

	@Override
	public Dialect getDialect() {
		return dialect;
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

	@Override
	public TypeInfo getTypeInfoForJdbcCode(int jdbcTypeCode) {
		for ( TypeInfo typeInfo : typeInfoSet ) {
			if ( typeInfo.getJdbcTypeCode() == jdbcTypeCode ) {
				return typeInfo;
			}
		}
		return null;
	}
}
