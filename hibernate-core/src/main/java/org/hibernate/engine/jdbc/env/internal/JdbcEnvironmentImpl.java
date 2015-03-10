/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.LobCreatorBuilder;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.TypeInfo;
import org.hibernate.exception.internal.SQLExceptionTypeDelegate;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.internal.StandardSQLExceptionConverter;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class JdbcEnvironmentImpl implements JdbcEnvironment {
	private final Dialect dialect;

	private final SqlExceptionHelper sqlExceptionHelper;
	private final ExtractedDatabaseMetaData extractedMetaDataSupport;
	private final Identifier currentCatalog;
	private final Identifier currentSchema;
	private final IdentifierHelper identifierHelper;
	private final QualifiedObjectNameFormatter qualifiedObjectNameFormatter;
	private final LobCreatorBuilderImpl lobCreatorBuilder;

	private final LinkedHashSet<TypeInfo> typeInfoSet = new LinkedHashSet<TypeInfo>();
	// todo : should really maintain a standard list of know ANSI-SQL defined keywords somewhere (currently rely on Dialect)
	private final Set<String> reservedWords = new HashSet<String>();

	/**
	 * Constructor form used when the JDBC {@link java.sql.DatabaseMetaData} is not available.
	 *
	 * @param serviceRegistry The service registry
	 * @param dialect The resolved dialect.
	 */
	public JdbcEnvironmentImpl(ServiceRegistryImplementor serviceRegistry, Dialect dialect) {
		this.dialect = dialect;

		this.sqlExceptionHelper = buildSqlExceptionHelper( dialect );
		this.extractedMetaDataSupport = new ExtractedDatabaseMetaDataImpl.Builder( this ).build();

		for ( String keyword : dialect.getKeywords() ) {
			reservedWords.add( keyword.toUpperCase() );
		}

		final boolean globallyQuoteIdentifiers = serviceRegistry.getService( ConfigurationService.class )
				.getSetting( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, StandardConverters.BOOLEAN, false );

		// a simple impl that works on H2
		this.identifierHelper = new NormalizingIdentifierHelperImpl(
				this,
				globallyQuoteIdentifiers,
				true,	// storesMixedCaseQuotedIdentifiers
				false,	// storesLowerCaseQuotedIdentifiers
				false, 	// storesUpperCaseQuotedIdentifiers
				true,	// storesUpperCaseIdentifiers
				false	// storesLowerCaseIdentifiers
		);

		this.currentCatalog = identifierHelper.toIdentifier(
				serviceRegistry.getService( ConfigurationService.class )
						.getSetting( AvailableSettings.DEFAULT_CATALOG, StandardConverters.STRING )
		);
		this.currentSchema = Identifier.toIdentifier(
				serviceRegistry.getService( ConfigurationService.class )
						.getSetting( AvailableSettings.DEFAULT_SCHEMA, StandardConverters.STRING )
		);

		// again, a simple impl that works on H2
		this.qualifiedObjectNameFormatter = new QualifiedObjectNameFormatterStandardImpl();

		this.lobCreatorBuilder = LobCreatorBuilderImpl.makeLobCreatorBuilder();
	}

	/**
	 * Constructor form used from testing
	 *
	 * @param dialect The dialect
	 */
	public JdbcEnvironmentImpl(DatabaseMetaData databaseMetaData, Dialect dialect) throws SQLException {
		this.dialect = dialect;

		this.sqlExceptionHelper = buildSqlExceptionHelper( dialect );

		this.extractedMetaDataSupport = new ExtractedDatabaseMetaDataImpl.Builder( this )
				.apply( databaseMetaData )
				.build();

		for ( String keyword : dialect.getKeywords() ) {
			reservedWords.add( keyword.toUpperCase() );
		}
		// ExtractedMetaDataSupport already capitalizes them
		reservedWords.addAll( extractedMetaDataSupport.getExtraKeywords() );

		final boolean globallyQuoteIdentifiers = false;

		// a simple impl that works on H2
		this.identifierHelper = new NormalizingIdentifierHelperImpl(
				this,
				globallyQuoteIdentifiers,
				true,	// storesMixedCaseQuotedIdentifiers
				false,	// storesLowerCaseQuotedIdentifiers
				false, 	// storesUpperCaseQuotedIdentifiers
				true,	// storesUpperCaseIdentifiers
				false	// storesLowerCaseIdentifiers
		);

		this.currentCatalog = null;
		this.currentSchema = null;

		this.qualifiedObjectNameFormatter = new QualifiedObjectNameFormatterStandardImpl( databaseMetaData );

		this.lobCreatorBuilder = LobCreatorBuilderImpl.makeLobCreatorBuilder();
	}

	public JdbcEnvironmentImpl(
			ServiceRegistryImplementor serviceRegistry,
			Dialect dialect,
			DatabaseMetaData databaseMetaData) throws SQLException {
		this.dialect = dialect;

		this.sqlExceptionHelper = buildSqlExceptionHelper( dialect );

		this.extractedMetaDataSupport = new ExtractedDatabaseMetaDataImpl.Builder( this )
				.apply( databaseMetaData )
				.setConnectionSchemaName( determineCurrentSchemaName( databaseMetaData, serviceRegistry, dialect ) )
				.build();

		for ( String keyword : dialect.getKeywords() ) {
			reservedWords.add( keyword.toUpperCase() );
		}
		// ExtractedMetaDataSupport already capitalizes them
		reservedWords.addAll( extractedMetaDataSupport.getExtraKeywords() );

		final boolean globallyQuoteIdentifiers = serviceRegistry.getService( ConfigurationService.class )
				.getSetting( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, StandardConverters.BOOLEAN, false );

		this.identifierHelper = new NormalizingIdentifierHelperImpl(
				this,
				globallyQuoteIdentifiers,
				databaseMetaData.storesMixedCaseQuotedIdentifiers(),
				databaseMetaData.storesLowerCaseQuotedIdentifiers(),
				databaseMetaData.storesUpperCaseQuotedIdentifiers(),
				databaseMetaData.storesUpperCaseIdentifiers(),
				databaseMetaData.storesLowerCaseIdentifiers()
		);

		// and that current-catalog and current-schema happen after it
		this.currentCatalog = identifierHelper.toIdentifier( extractedMetaDataSupport.getConnectionCatalogName() );
		this.currentSchema = identifierHelper.toIdentifier( extractedMetaDataSupport.getConnectionSchemaName() );

		this.qualifiedObjectNameFormatter = new QualifiedObjectNameFormatterStandardImpl( databaseMetaData );

		this.typeInfoSet.addAll( TypeInfo.extractTypeInfo( databaseMetaData ) );

		this.lobCreatorBuilder = LobCreatorBuilderImpl.makeLobCreatorBuilder(
				serviceRegistry.getService( ConfigurationService.class ).getSettings(),
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
	private SqlExceptionHelper buildSqlExceptionHelper(Dialect dialect) {
		final StandardSQLExceptionConverter sqlExceptionConverter = new StandardSQLExceptionConverter();
		sqlExceptionConverter.addDelegate( dialect.buildSQLExceptionConversionDelegate() );
		sqlExceptionConverter.addDelegate( new SQLExceptionTypeDelegate( dialect ) );
		// todo : vary this based on extractedMetaDataSupport.getSqlStateType()
		sqlExceptionConverter.addDelegate( new SQLStateConversionDelegate( dialect ) );
		return new SqlExceptionHelper( sqlExceptionConverter );
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
	public boolean isReservedWord(String word) {
		return reservedWords.contains( word.toUpperCase() );
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
