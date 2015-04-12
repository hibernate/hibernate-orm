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

import java.util.Locale;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;

import org.jboss.logging.Logger;

/**
* @author Steve Ebersole
*/
public class NormalizingIdentifierHelperImpl implements IdentifierHelper {
	private static final Logger log = Logger.getLogger( NormalizingIdentifierHelperImpl.class );

	private final JdbcEnvironment jdbcEnvironment;

	private final NameQualifierSupport nameQualifierSupport;
	private final boolean globallyQuoteIdentifiers;

	private final boolean storesMixedCaseQuotedIdentifiers;
	private final boolean storesLowerCaseQuotedIdentifiers;
	private final boolean storesUpperCaseQuotedIdentifiers;
	private final boolean storesMixedCaseIdentifiers;
	private final boolean storesUpperCaseIdentifiers;
	private final boolean storesLowerCaseIdentifiers;

	public NormalizingIdentifierHelperImpl(
			JdbcEnvironment jdbcEnvironment,
			NameQualifierSupport nameQualifierSupport,
			boolean globallyQuoteIdentifiers,
			boolean storesMixedCaseQuotedIdentifiers,
			boolean storesLowerCaseQuotedIdentifiers,
			boolean storesUpperCaseQuotedIdentifiers,
			boolean storesMixedCaseIdentifiers,
			boolean storesUpperCaseIdentifiers,
			boolean storesLowerCaseIdentifiers) {
		this.jdbcEnvironment = jdbcEnvironment;
		this.nameQualifierSupport = nameQualifierSupport;
		this.globallyQuoteIdentifiers = globallyQuoteIdentifiers;
		this.storesMixedCaseQuotedIdentifiers = storesMixedCaseQuotedIdentifiers;
		this.storesLowerCaseQuotedIdentifiers = storesLowerCaseQuotedIdentifiers;
		this.storesUpperCaseQuotedIdentifiers = storesUpperCaseQuotedIdentifiers;
		this.storesMixedCaseIdentifiers = storesMixedCaseIdentifiers;
		this.storesUpperCaseIdentifiers = storesUpperCaseIdentifiers;
		this.storesLowerCaseIdentifiers = storesLowerCaseIdentifiers;

		if ( storesMixedCaseQuotedIdentifiers && storesLowerCaseQuotedIdentifiers && storesUpperCaseQuotedIdentifiers ) {
			log.warn( "JDBC Driver reports it stores quoted identifiers in mixed, upper and lower case" );
		}
		else if ( storesMixedCaseQuotedIdentifiers && storesUpperCaseQuotedIdentifiers ) {
			log.warn( "JDBC Driver reports it stores quoted identifiers in both mixed and upper case" );
		}
		else if ( storesMixedCaseQuotedIdentifiers && storesLowerCaseQuotedIdentifiers ) {
			log.warn( "JDBC Driver reports it stores quoted identifiers in both mixed and lower case" );
		}

		if ( storesUpperCaseIdentifiers && storesLowerCaseIdentifiers ) {
			log.warn( "JDBC Driver reports it stores non-quoted identifiers in both upper and lower case" );
		}

		if ( storesUpperCaseIdentifiers && storesUpperCaseQuotedIdentifiers ) {
			log.warn( "JDBC Driver reports it stores both quoted and non-quoted identifiers in upper case" );
		}

		if ( storesLowerCaseIdentifiers && storesLowerCaseQuotedIdentifiers ) {
			log.warn( "JDBC Driver reports it stores both quoted and non-quoted identifiers in lower case" );
		}
	}

	@Override
	public Identifier normalizeQuoting(Identifier identifier) {
		if ( identifier == null ) {
			return null;
		}

		if ( identifier.isQuoted() ) {
			return identifier;
		}

		if ( globallyQuoteIdentifiers ) {
			return Identifier.toIdentifier( identifier.getText(), true );
		}

		if ( jdbcEnvironment.isReservedWord( identifier.getText() ) ) {
			return Identifier.toIdentifier( identifier.getText(), true );
		}

		return identifier;
	}

	@Override
	public Identifier toIdentifier(String text) {
		return normalizeQuoting( Identifier.toIdentifier( text ) );
	}

	@Override
	public Identifier toIdentifier(String text, boolean quoted) {
		return normalizeQuoting( Identifier.toIdentifier( text, quoted ) );
	}

	// In the DatabaseMetaData method params for catalog and schema name have the following meaning:
	//		1) <""> means to match things "without a catalog/schema"
	//		2) <null> means to not limit results based on this field
	//
	// todo : not sure how "without a catalog/schema" is interpreted.  Current?

	@Override
	public String toMetaDataCatalogName(Identifier identifier) {
		if ( !nameQualifierSupport.supportsCatalogs() ) {
			// null is used to tell DBMD to not limit results based on catalog.
			return null;
		}

		if ( identifier == null ) {
			if ( jdbcEnvironment.getCurrentCatalog() == null ) {
				return "";
			}
			identifier = jdbcEnvironment.getCurrentCatalog();
		}

		return toMetaDataText( identifier );
	}

	private String toMetaDataText(Identifier identifier) {
		if ( identifier == null ) {
			throw new IllegalArgumentException( "Identifier cannot be null; bad usage" );
		}

		if ( identifier.isQuoted() ) {
			if ( storesMixedCaseQuotedIdentifiers ) {
				return identifier.getText();
			}
			else if ( storesUpperCaseQuotedIdentifiers ) {
				return identifier.getText().toUpperCase( Locale.ENGLISH );
			}
			else if ( storesLowerCaseQuotedIdentifiers ) {
				return identifier.getText().toLowerCase( Locale.ENGLISH );
			}
			else {
				return identifier.getText();
			}
		}
		else {
			if ( storesMixedCaseIdentifiers ) {
				return identifier.getText();
			}
			else if ( storesUpperCaseIdentifiers ) {
				return identifier.getText().toUpperCase( Locale.ENGLISH );
			}
			else if ( storesLowerCaseIdentifiers ) {
				return identifier.getText().toLowerCase( Locale.ENGLISH );
			}
			else {
				return identifier.getText();
			}
		}
	}

	@Override
	public String toMetaDataSchemaName(Identifier identifier) {
		if ( !nameQualifierSupport.supportsSchemas() ) {
			// null is used to tell DBMD to not limit results based on schema.
			return null;
		}

		if ( identifier == null ) {
			if ( jdbcEnvironment.getCurrentSchema() == null ) {
				return "";
			}
			identifier = jdbcEnvironment.getCurrentSchema();
		}

		return toMetaDataText( identifier );
	}

	@Override
	public String toMetaDataObjectName(Identifier identifier) {
		if ( identifier == null ) {
			// if this method was called, the value is needed
			throw new IllegalArgumentException( "null was passed as an object name" );
		}
		return toMetaDataText( identifier );
	}

	@Override
	public Identifier fromMetaDataCatalogName(String catalogName) {
		if ( catalogName == null || !nameQualifierSupport.supportsCatalogs() ) {
			return null;
		}

//		if ( jdbcEnvironment.getCurrentCatalog() == null
//				|| catalogName.equals( jdbcEnvironment.getCurrentCatalog().getText() ) ) {
//			return null;
//		}

		return toIdentifierFromMetaData( catalogName );
	}

	public Identifier toIdentifierFromMetaData(String text) {
		if ( globallyQuoteIdentifiers ) {
			return Identifier.toIdentifier( text, true );
		}

		// lovely decipher of whether the incoming value represents a quoted identifier...
		final boolean isUpperCase = text.toUpperCase(Locale.ROOT).equals( text );
		final boolean isLowerCase = text.toLowerCase(Locale.ROOT).equals( text );
		final boolean isMixedCase = ! isLowerCase && ! isUpperCase;

		if ( jdbcEnvironment.isReservedWord( text ) ) {
			// unequivocally it needs to be quoted...
			return Identifier.toIdentifier( text, true );
		}

		if ( storesMixedCaseQuotedIdentifiers && isMixedCase ) {
			return Identifier.toIdentifier( text, true );
		}

		if ( storesLowerCaseQuotedIdentifiers && isLowerCase ) {
			return Identifier.toIdentifier( text, true );
		}

		if ( storesUpperCaseQuotedIdentifiers && isUpperCase ) {
			return Identifier.toIdentifier( text, true );
		}

		return Identifier.toIdentifier( text );
	}

	@Override
	public Identifier fromMetaDataSchemaName(String schemaName) {
		if ( schemaName == null || !nameQualifierSupport.supportsSchemas() ) {
			return null;
		}

//		if ( jdbcEnvironment.getCurrentSchema() == null
//				|| schemaName.equals( jdbcEnvironment.getCurrentSchema().getText() ) ) {
//			return null;
//		}

		return toIdentifier( schemaName );
	}

	@Override
	public Identifier fromMetaDataObjectName(String objectName) {
		if ( objectName == null ) {
			return null;
		}

		return toIdentifier( objectName );
	}
}
