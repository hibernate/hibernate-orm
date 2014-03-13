/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import java.lang.reflect.InvocationTargetException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.cursor.internal.StandardRefCursorSupport;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.LobCreatorBuilder;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameSupport;
import org.hibernate.engine.jdbc.env.spi.SQLStateType;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.jdbc.env.spi.StandardQualifiedObjectNameSupportImpl;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.TypeInfo;
import org.hibernate.exception.internal.SQLExceptionTypeDelegate;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.internal.StandardSQLExceptionConverter;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class JdbcEnvironmentImpl implements JdbcEnvironment {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( JdbcEnvironmentImpl.class );

	private final ServiceRegistryImplementor serviceRegistry;
	private final Dialect dialect;

	private final SqlExceptionHelper sqlExceptionHelper;
	private final ExtractedDatabaseMetaData extractedMetaDataSupport;
	private final Set<String> reservedWords;
	private final Identifier currentCatalog;
	private final Identifier currentSchema;
	private final IdentifierHelper identifierHelper;
	private final QualifiedObjectNameSupport qualifiedObjectNameSupport;
	private final LobCreatorBuilderImpl lobCreatorBuilder;
	private final LinkedHashSet<TypeInfo> typeInfoSet = new LinkedHashSet<TypeInfo>();

	/**
	 * Constructor form used when the JDBC {@link DatabaseMetaData} is not available.
	 *
	 * @param serviceRegistry The service registry
	 * @param dialect The resolved dialect.
	 */
	public JdbcEnvironmentImpl(ServiceRegistryImplementor serviceRegistry, Dialect dialect) {
		this.serviceRegistry = serviceRegistry;
		this.dialect = dialect;

		this.sqlExceptionHelper = buildSqlExceptionHelper( dialect );
		this.extractedMetaDataSupport = new ExtractedDatabaseMetaDataImpl( this );

		// make sure reserved-words, current-catalog and current-schema happen before the identifier-helper!
		this.reservedWords = dialect.getKeywords();
		this.currentCatalog = Identifier.toIdentifier(
				serviceRegistry.getService( ConfigurationService.class )
						.getSetting( AvailableSettings.DEFAULT_CATALOG, StandardConverters.STRING )
		);
		this.currentSchema = Identifier.toIdentifier(
				serviceRegistry.getService( ConfigurationService.class )
						.getSetting( AvailableSettings.DEFAULT_SCHEMA, StandardConverters.STRING )
		);

		final boolean globallyQuoteIdentifiers = serviceRegistry.getService( ConfigurationService.class )
				.getSetting( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, StandardConverters.BOOLEAN, false );

		// a simple temporary impl that works on H2
		this.identifierHelper = new NormalizingIdentifierHelperImpl(
				this,
				globallyQuoteIdentifiers,
				true,	// storesMixedCaseQuotedIdentifiers
				false,	// storesLowerCaseQuotedIdentifiers
				false, 	// storesUpperCaseQuotedIdentifiers
				true,	// storesUpperCaseIdentifiers
				false	// storesLowerCaseIdentifiers
		);

		// again, a simple temporary impl that works on H2
		this.qualifiedObjectNameSupport = new StandardQualifiedObjectNameSupportImpl(
				".",
				true,
				dialect.openQuote(),
				dialect.closeQuote()
		);

		this.lobCreatorBuilder = LobCreatorBuilderImpl.makeLobCreatorBuilder();

	}

	public JdbcEnvironmentImpl(ServiceRegistryImplementor serviceRegistry, Dialect dialect, DatabaseMetaData dbmd) throws SQLException {
		this.serviceRegistry = serviceRegistry;
		this.dialect = dialect;

		this.sqlExceptionHelper = buildSqlExceptionHelper( dialect );

		this.extractedMetaDataSupport = new ExtractedDatabaseMetaDataImpl(
				this,
				StandardRefCursorSupport.supportsRefCursors( dbmd ),
				dbmd.supportsNamedParameters(),
				dbmd.supportsResultSetType( ResultSet.TYPE_SCROLL_INSENSITIVE ),
				dbmd.supportsGetGeneratedKeys(),
				dbmd.supportsBatchUpdates(),
				!dbmd.dataDefinitionIgnoredInTransactions(),
				dbmd.dataDefinitionCausesTransactionCommit(),
				parseSQLStateType( dbmd.getSQLStateType() ),
				dbmd.locatorsUpdateCopy()
		);

		// make sure reserved-words happen before the identifier-helper!
		this.reservedWords = buildMergedReservedWords( dialect, dbmd );

		final boolean globallyQuoteIdentifiers = serviceRegistry.getService( ConfigurationService.class )
				.getSetting( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, StandardConverters.BOOLEAN, false );

		this.identifierHelper = new NormalizingIdentifierHelperImpl(
				this,
				globallyQuoteIdentifiers,
				dbmd.storesMixedCaseQuotedIdentifiers(),
				dbmd.storesLowerCaseQuotedIdentifiers(),
				dbmd.storesUpperCaseQuotedIdentifiers(),
				dbmd.storesUpperCaseIdentifiers(),
				dbmd.storesLowerCaseIdentifiers()
		);

		// and that current-catalog and current-schema happen after it
		this.currentCatalog = determineCurrentCatalog( dbmd );
		this.currentSchema = determineCurrentSchema( dbmd );

		this.qualifiedObjectNameSupport = new StandardQualifiedObjectNameSupportImpl(
				dbmd.getCatalogSeparator(),
				dbmd.isCatalogAtStart(),
				dialect.openQuote(),
				dialect.closeQuote()
		);

		this.typeInfoSet.addAll( TypeInfo.extractTypeInfo( dbmd ) );

		this.lobCreatorBuilder = LobCreatorBuilderImpl.makeLobCreatorBuilder(
				serviceRegistry.getService( ConfigurationService.class ).getSettings(),
				dbmd.getConnection()
		);
	}

	private SQLStateType parseSQLStateType(int sqlStateType) {
		switch ( sqlStateType ) {
			case DatabaseMetaData.sqlStateSQL99 : {
				return SQLStateType.SQL99;
			}
			case DatabaseMetaData.sqlStateXOpen : {
				return SQLStateType.XOpen;
			}
			default : {
				return SQLStateType.UNKNOWN;
			}
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

	private Identifier determineCurrentCatalog(DatabaseMetaData dbmd) throws SQLException {
		String currentCatalogName = dbmd.getConnection().getCatalog();
		if ( currentCatalogName != null ) {
			// intentionally using fromMetaDataObjectName rather than fromMetaDataCatalogName !!!
			return identifierHelper.fromMetaDataObjectName( currentCatalogName );
		}
		else {
			currentCatalogName = serviceRegistry.getService( ConfigurationService.class )
					.getSetting( AvailableSettings.DEFAULT_CATALOG, StandardConverters.STRING );
			return Identifier.toIdentifier( currentCatalogName );
		}
	}

	private Identifier determineCurrentSchema(DatabaseMetaData dbmd) throws SQLException {
		String currentSchemaName = locateSchemaNameResolver().resolveSchemaName( dbmd.getConnection(), dialect );
		if ( currentSchemaName != null ) {
			// intentionally using fromMetaDataObjectName rather than fromMetaDataSchemaName !!!
			return identifierHelper.fromMetaDataObjectName( currentSchemaName );
		}
		else {
			currentSchemaName = serviceRegistry.getService( ConfigurationService.class )
					.getSetting( AvailableSettings.DEFAULT_SCHEMA, StandardConverters.STRING );
			return Identifier.toIdentifier( currentSchemaName );
		}
	}

	private SchemaNameResolver locateSchemaNameResolver() {
		final Object setting = serviceRegistry.getService( ConfigurationService.class )
				.getSettings()
				.get( AvailableSettings.SCHEMA_NAME_RESOLVER );
		try {
			return serviceRegistry.getService( StrategySelector.class ).resolveDefaultableStrategy(
					SchemaNameResolver.class,
					setting,
					DefaultSchemaNameResolver.INSTANCE
			);
		}
		catch ( StrategySelectionException e ) {
			final Throwable cause = e.getCause();
			if ( ClassNotFoundException.class.isInstance( cause ) ) {
				LOG.unableToLocateConfiguredSchemaNameResolver(
						e.getImplementationClassName(),
						cause.toString()
				);
			}
			else if ( InvocationTargetException.class.isInstance( cause ) ) {
				LOG.unableToInstantiateConfiguredSchemaNameResolver(
						e.getImplementationClassName(),
						InvocationTargetException.class.cast( cause ).getTargetException().toString() );
			}
			else {
				LOG.unableToInstantiateConfiguredSchemaNameResolver(
						e.getImplementationClassName(),
						cause.toString() );
			}
			return null;
		}
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
	public QualifiedObjectNameSupport getQualifiedObjectNameSupport() {
		return qualifiedObjectNameSupport;
	}

	@Override
	public IdentifierHelper getIdentifierHelper() {
		return identifierHelper;
	}

	@Override
	public Set<String> getReservedWords() {
		return reservedWords;
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
	
	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}
}
