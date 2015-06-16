/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.internal;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
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
	private final boolean autoQuoteKeywords;
	private final Set<String> reservedWords = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
	private final IdentifierCaseStrategy unquotedCaseStrategy;
	private final IdentifierCaseStrategy quotedCaseStrategy;

	public NormalizingIdentifierHelperImpl(
			JdbcEnvironment jdbcEnvironment,
			NameQualifierSupport nameQualifierSupport,
			boolean globallyQuoteIdentifiers,
			boolean autoQuoteKeywords,
			Set<String> reservedWords,
			IdentifierCaseStrategy unquotedCaseStrategy,
			IdentifierCaseStrategy quotedCaseStrategy) {
		this.jdbcEnvironment = jdbcEnvironment;
		this.nameQualifierSupport = nameQualifierSupport;
		this.globallyQuoteIdentifiers = globallyQuoteIdentifiers;
		this.autoQuoteKeywords = autoQuoteKeywords;
		if ( reservedWords != null ) {
			this.reservedWords.addAll( reservedWords );
		}
		this.unquotedCaseStrategy = unquotedCaseStrategy == null ? IdentifierCaseStrategy.UPPER : unquotedCaseStrategy;
		this.quotedCaseStrategy = quotedCaseStrategy == null ? IdentifierCaseStrategy.MIXED : quotedCaseStrategy;
	}

	@Override
	public Identifier normalizeQuoting(Identifier identifier) {
		log.tracef( "Normalizing identifier quoting [%s]", identifier );

		if ( identifier == null ) {
			return null;
		}

		if ( identifier.isQuoted() ) {
			return identifier;
		}

		if ( globallyQuoteIdentifiers ) {
			log.tracef( "Forcing identifier [%s] to quoted for global quoting", identifier );
			return Identifier.toIdentifier( identifier.getText(), true );
		}

		if ( autoQuoteKeywords && isReservedWord( identifier.getText() ) ) {
			log.tracef( "Forcing identifier [%s] to quoted as recognized reserveed word", identifier );
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

	@Override
	public Identifier applyGlobalQuoting(String text) {
		return Identifier.toIdentifier( text, globallyQuoteIdentifiers );
	}

	@Override
	public boolean isReservedWord(String word) {
		return reservedWords.contains( word );
	}

	@Override
	public String toMetaDataCatalogName(Identifier identifier) {
		log.tracef( "Normalizing identifier quoting for catalog name [%s]", identifier );

		if ( !nameQualifierSupport.supportsCatalogs() ) {
			log.trace( "Environment does not support catalogs; returning null" );
			// null is used to tell DatabaseMetaData to not limit results based on catalog.
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
			switch ( quotedCaseStrategy ) {
				case UPPER: {
					log.tracef( "Rendering quoted identifier [%s] in upper case for use in DatabaseMetaData", identifier );
					return identifier.getText().toUpperCase( Locale.ROOT );
				}
				case LOWER: {
					log.tracef( "Rendering quoted identifier [%s] in lower case for use in DatabaseMetaData", identifier );
					return identifier.getText().toLowerCase( Locale.ROOT );
				}
				default: {
					// default is mixed case
					log.tracef( "Rendering quoted identifier [%s] in mixed case for use in DatabaseMetaData", identifier );
					return identifier.getText();
				}
			}
		}
		else {
			switch ( unquotedCaseStrategy ) {
				case MIXED: {
					log.tracef( "Rendering unquoted identifier [%s] in mixed case for use in DatabaseMetaData", identifier );
					return identifier.getText();
				}
				case LOWER: {
					log.tracef( "Rendering unquoted identifier [%s] in lower case for use in DatabaseMetaData", identifier );
					return identifier.getText().toLowerCase( Locale.ROOT );
				}
				default: {
					// default is upper case
					log.tracef( "Rendering unquoted identifier [%s] in upper case for use in DatabaseMetaData", identifier );
					return identifier.getText().toUpperCase( Locale.ROOT );
				}
			}
		}
	}

	@Override
	public String toMetaDataSchemaName(Identifier identifier) {
		log.tracef( "Normalizing identifier quoting for schema name [%s]", identifier );

		if ( !nameQualifierSupport.supportsSchemas() ) {
			// null is used to tell DatabaseMetaData to not limit results based on schema.
			log.trace( "Environment does not support catalogs; returning null" );
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
		log.tracef( "Normalizing identifier quoting for object name [%s]", identifier );

		if ( identifier == null ) {
			// if this method was called, the value is needed
			throw new IllegalArgumentException( "null was passed as an object name" );
		}
		return toMetaDataText( identifier );
	}
}
