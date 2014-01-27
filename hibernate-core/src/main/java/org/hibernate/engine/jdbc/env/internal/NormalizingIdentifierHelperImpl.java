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

import org.jboss.logging.Logger;

import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.relational.Identifier;

/**
* @author Steve Ebersole
*/
public class NormalizingIdentifierHelperImpl implements IdentifierHelper {
	private static final Logger log = Logger.getLogger( NormalizingIdentifierHelperImpl.class );

	private final JdbcEnvironment jdbcEnvironment;

	private final boolean globallyQuoteIdentifiers;

	private final boolean storesMixedCaseQuotedIdentifiers;
	private final boolean storesLowerCaseQuotedIdentifiers;
	private final boolean storesUpperCaseQuotedIdentifiers;
	private final boolean storesUpperCaseIdentifiers;
	private final boolean storesLowerCaseIdentifiers;

	public NormalizingIdentifierHelperImpl(
			JdbcEnvironment jdbcEnvironment,
			boolean globallyQuoteIdentifiers,
			boolean storesMixedCaseQuotedIdentifiers,
			boolean storesLowerCaseQuotedIdentifiers,
			boolean storesUpperCaseQuotedIdentifiers,
			boolean storesUpperCaseIdentifiers,
			boolean storesLowerCaseIdentifiers) {
		this.jdbcEnvironment = jdbcEnvironment;
		this.globallyQuoteIdentifiers = globallyQuoteIdentifiers;
		this.storesMixedCaseQuotedIdentifiers = storesMixedCaseQuotedIdentifiers;
		this.storesLowerCaseQuotedIdentifiers = storesLowerCaseQuotedIdentifiers;
		this.storesUpperCaseQuotedIdentifiers = storesUpperCaseQuotedIdentifiers;
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

	// In the DatabaseMetaData method params for catalog and schema name have the following meaning:
	//		1) <""> means to match things "without a catalog/schema"
	//		2) <null> means to not limit results based on this field
	//
	// todo : not sure how "without a catalog/schema" is interpreted.  Current?

	@Override
	public String toMetaDataCatalogName(Identifier identifier) {
		if ( identifier == null ) {
			// todo : not sure if this is interpreted as <""> or <currentCatalog>
			return jdbcEnvironment.getCurrentCatalog() == null ? null : jdbcEnvironment.getCurrentCatalog().getText();
		}

		return toText( identifier );
	}

	private String toText(Identifier identifier) {
		if ( identifier == null ) {
			throw new IllegalArgumentException( "Identifier cannot be null; bad usage" );
		}

		if ( identifier.isQuoted() && storesMixedCaseQuotedIdentifiers ) {
			return identifier.getText();
		}
		else if ( ( identifier.isQuoted() && storesUpperCaseQuotedIdentifiers )
				|| ( !identifier.isQuoted() && storesUpperCaseIdentifiers ) ) {
			return StringHelper.toUpperCase( identifier.getText() );
		}
		else if ( ( identifier.isQuoted() && storesLowerCaseQuotedIdentifiers )
				|| ( !identifier.isQuoted() && storesLowerCaseIdentifiers ) ) {
			return StringHelper.toLowerCase( identifier.getText() );
		}
		return identifier.getText();
	}

	@Override
	public String toMetaDataSchemaName(Identifier identifier) {
		if ( identifier == null ) {
			// todo : not sure if this is interpreted as <""> or <currentSchema>
			return jdbcEnvironment.getCurrentSchema() == null ? null : jdbcEnvironment.getCurrentSchema().getText();
		}

		return toText( identifier );
	}

	@Override
	public String toMetaDataObjectName(Identifier identifier) {
		if ( identifier == null ) {
			// if this method was called, the value is needed
			throw new IllegalArgumentException(  );
		}
		return toText( identifier );
	}

	@Override
	public Identifier fromMetaDataCatalogName(String catalogName) {
		if ( catalogName == null ) {
			return null;
		}

		if ( jdbcEnvironment.getCurrentCatalog() == null
				|| catalogName.equals( jdbcEnvironment.getCurrentCatalog().getText() ) ) {
			return null;
		}

		return toIdentifier( catalogName );
		// note really sure the best way to know (can you?) whether the identifier is quoted

	}

	public Identifier toIdentifier(String text) {
		if ( globallyQuoteIdentifiers ) {
			return Identifier.toIdentifier( text, true );
		}

		// lovely decipher of whether the incoming value represents a quoted identifier...
		final boolean isUpperCase = text.toUpperCase().equals( text );
		final boolean isLowerCase = text.toLowerCase().equals( text );
		final boolean isMixedCase = ! isLowerCase && ! isUpperCase;

		if ( jdbcEnvironment.getReservedWords().contains( text ) ) {
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
	public Identifier toIdentifier(String text, boolean quoted) {
		return globallyQuoteIdentifiers
				? Identifier.toIdentifier( text, true )
				: Identifier.toIdentifier( text, quoted );
	}

	@Override
	public Identifier fromMetaDataSchemaName(String schemaName) {
		if ( schemaName == null ) {
			return null;
		}

		if ( jdbcEnvironment.getCurrentSchema() == null
				|| schemaName.equals( jdbcEnvironment.getCurrentSchema().getText() ) ) {
			return null;
		}

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
