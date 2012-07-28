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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.SchemaCatalogSupport;
import org.hibernate.engine.jdbc.env.spi.StandardSchemaCatalogSupportImpl;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.exception.internal.SQLExceptionTypeDelegate;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.internal.StandardSQLExceptionConverter;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.service.schema.spi.ExistingSequenceMetadataExtractor;

/**
 * @author Steve Ebersole
 */
public class JdbcEnvironmentImpl implements JdbcEnvironment {
	private final Dialect dialect;
	private final IdentifierHelper identifierHelper;
	private final Identifier currentCatalog;
	private final Identifier currentSchema;
	private final SchemaCatalogSupport schemaCatalogSupport;
	private ExistingSequenceMetadataExtractor sequenceMetadataExtractor;
	private final Set<String> reservedWords;
	private final SqlExceptionHelper sqlExceptionHelper;

	public JdbcEnvironmentImpl(DatabaseMetaData dbmd, Dialect dialect, Map properties) throws SQLException {
		this.dialect = dialect;

		Set<String> reservedWords = new HashSet<String>();
		reservedWords.addAll( dialect.getKeywords() );
		// todo : do we need to explicitly handle SQL:2003 keywords?
		reservedWords.addAll( Arrays.asList( dbmd.getSQLKeywords().split( "," ) ) );
		this.reservedWords = reservedWords;

		this.identifierHelper = new NormalizingIdentifierHelperImpl(
				this,
				dbmd.storesMixedCaseQuotedIdentifiers(),
				dbmd.storesLowerCaseQuotedIdentifiers(),
				dbmd.storesUpperCaseQuotedIdentifiers(),
				dbmd.storesUpperCaseIdentifiers(),
				dbmd.storesLowerCaseIdentifiers()
		);

		String currentCatalogName = dbmd.getConnection().getCatalog();
		if ( currentCatalogName != null ) {
			// intentionally using fromMetaDataObjectName rather than fromMetaDataCatalogName !!!
			currentCatalog = identifierHelper.fromMetaDataObjectName( currentCatalogName );
		}
		else {
			currentCatalogName = (String) properties.get( AvailableSettings.DEFAULT_CATALOG );
			currentCatalog = Identifier.toIdentifier( currentCatalogName );
		}

		String currentSchemaName = TemporarySchemaNameResolver.INSTANCE.resolveSchemaName( dbmd.getConnection() );
		if ( currentSchemaName != null ) {
			// intentionally using fromMetaDataObjectName rather than fromMetaDataSchemaName !!!
			currentSchema = identifierHelper.fromMetaDataObjectName( currentSchemaName );
		}
		else {
			currentSchemaName = (String) properties.get( AvailableSettings.DEFAULT_SCHEMA );
			currentSchema = Identifier.toIdentifier( currentSchemaName );
		}

		schemaCatalogSupport = new StandardSchemaCatalogSupportImpl(
				dbmd.getCatalogSeparator(),
				dbmd.isCatalogAtStart(),
				dialect.openQuote(),
				dialect.closeQuote()
		);

		SQLExceptionConverter sqlExceptionConverter = dialect.buildSQLExceptionConverter();
		if ( sqlExceptionConverter == null ) {
			final StandardSQLExceptionConverter converter = new StandardSQLExceptionConverter();
			sqlExceptionConverter = converter;
			converter.addDelegate( dialect.buildSQLExceptionConversionDelegate() );
			converter.addDelegate( new SQLExceptionTypeDelegate( dialect ) );
			converter.addDelegate( new SQLStateConversionDelegate( dialect ) );
		}
		this.sqlExceptionHelper = new SqlExceptionHelper( sqlExceptionConverter );

		this.sequenceMetadataExtractor = new TemporaryExistingSequenceMetadataExtractor( this );
	}

	public JdbcEnvironmentImpl(Dialect dialect, Map properties) {
		this.dialect = dialect;

		Set<String> reservedWords = new HashSet<String>();
		reservedWords.addAll( dialect.getKeywords() );
		// todo : do we need to explicitly handle SQL:2003 keywords?
		this.reservedWords = reservedWords;

		// again, a simple temporary impl that works on H2
		this.identifierHelper = new NormalizingIdentifierHelperImpl(
				this,
				true,	// storesMixedCaseQuotedIdentifiers
				false,	// storesLowerCaseQuotedIdentifiers
				false, 	// storesUpperCaseQuotedIdentifiers
				true,	// storesUpperCaseIdentifiers
				false	// storesLowerCaseIdentifiers
		);

		String currentCatalogName = (String) properties.get( AvailableSettings.DEFAULT_CATALOG );
		currentCatalog = Identifier.toIdentifier( currentCatalogName );

		String currentSchemaName = (String) properties.get( AvailableSettings.DEFAULT_SCHEMA );
		currentSchema = Identifier.toIdentifier( currentSchemaName );

		// again, a simple temporary impl that works on H2
		schemaCatalogSupport = new StandardSchemaCatalogSupportImpl(
				".",
				true,
				dialect.openQuote(),
				dialect.closeQuote()
		);

		SQLExceptionConverter sqlExceptionConverter = dialect.buildSQLExceptionConverter();
		if ( sqlExceptionConverter == null ) {
			final StandardSQLExceptionConverter converter = new StandardSQLExceptionConverter();
			sqlExceptionConverter = converter;
			converter.addDelegate( dialect.buildSQLExceptionConversionDelegate() );
			converter.addDelegate( new SQLExceptionTypeDelegate( dialect ) );
			converter.addDelegate( new SQLStateConversionDelegate( dialect ) );
		}
		this.sqlExceptionHelper = new SqlExceptionHelper( sqlExceptionConverter );

		this.sequenceMetadataExtractor = new TemporaryExistingSequenceMetadataExtractor( this );
	}

	@Override
	public Dialect getDialect() {
		return dialect;
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
	public SchemaCatalogSupport getSchemaCatalogSupport() {
		return schemaCatalogSupport;
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
	public ExistingSequenceMetadataExtractor getExistingSequenceMetadataExtractor() {
		return sequenceMetadataExtractor;
	}
}
