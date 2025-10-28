/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.internal;

import java.util.Locale;
import java.util.TreeSet;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.model.naming.DatabaseIdentifier;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;

/**
* @author Steve Ebersole
*/
public class NormalizingIdentifierHelperImpl implements IdentifierHelper {

	private final JdbcEnvironment jdbcEnvironment;

	private final NameQualifierSupport nameQualifierSupport;
	private final boolean globallyQuoteIdentifiers;
	private final boolean globallyQuoteIdentifiersSkipColumnDefinitions;
	private final boolean autoQuoteKeywords;
	private final boolean autoQuoteInitialUnderscore;
	private final boolean autoQuoteDollar;
	private final TreeSet<String> reservedWords;
	private final IdentifierCaseStrategy unquotedCaseStrategy;
	private final IdentifierCaseStrategy quotedCaseStrategy;

	public NormalizingIdentifierHelperImpl(
			JdbcEnvironment jdbcEnvironment,
			NameQualifierSupport nameQualifierSupport,
			boolean globallyQuoteIdentifiers,
			boolean globallyQuoteIdentifiersSkipColumnDefinitions,
			boolean autoQuoteKeywords,
			boolean autoQuoteInitialUnderscore,
			boolean autoQuoteDollar,
			TreeSet<String> reservedWords, //careful, we intentionally omit making a defensive copy to not waste memory
			IdentifierCaseStrategy unquotedCaseStrategy,
			IdentifierCaseStrategy quotedCaseStrategy) {
		this.jdbcEnvironment = jdbcEnvironment;
		this.nameQualifierSupport = nameQualifierSupport;
		this.globallyQuoteIdentifiers = globallyQuoteIdentifiers;
		this.globallyQuoteIdentifiersSkipColumnDefinitions = globallyQuoteIdentifiersSkipColumnDefinitions;
		this.autoQuoteKeywords = autoQuoteKeywords;
		this.autoQuoteInitialUnderscore = autoQuoteInitialUnderscore;
		this.autoQuoteDollar = autoQuoteDollar;
		this.reservedWords = reservedWords;
		this.unquotedCaseStrategy = unquotedCaseStrategy == null ? IdentifierCaseStrategy.UPPER : unquotedCaseStrategy;
		this.quotedCaseStrategy = quotedCaseStrategy == null ? IdentifierCaseStrategy.MIXED : quotedCaseStrategy;
	}

	@Override
	public Identifier normalizeQuoting(Identifier identifier) {
		if ( identifier == null ) {
			return null;
		}
		else if ( identifier.isQuoted() ) {
			return identifier;
		}
		else if ( mustQuote( identifier ) ) {
			return Identifier.toIdentifier( identifier.getText(), true );
		}
		else {
			return identifier;
		}
	}

	private boolean mustQuote(Identifier identifier) {
		final String identifierText = identifier.getText();
		return globallyQuoteIdentifiers
			|| autoQuoteKeywords && isReservedWord( identifierText )
			|| autoQuoteInitialUnderscore && identifierText.startsWith( "_" )
			|| autoQuoteDollar && identifierText.contains( "$" );
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
		return Identifier.toIdentifier( text, globallyQuoteIdentifiers && !globallyQuoteIdentifiersSkipColumnDefinitions, false );
	}

	@Override
	public boolean isReservedWord(String word) {
		if ( !autoQuoteKeywords ) {
			throw new AssertionFailure( "The reserved keywords map is only initialized if autoQuoteKeywords is true" );
		}
		return reservedWords.contains( word );
	}

	@Override
	public String toMetaDataCatalogName(Identifier identifier) {
		if ( !nameQualifierSupport.supportsCatalogs() ) {
			// null is used to tell DatabaseMetaData to not limit results based on catalog.
			return null;
		}
		else {
			final var id =
					identifier == null
							? jdbcEnvironment.getCurrentCatalog()
							: identifier;
			return id == null ? "" : toMetaDataText( id );
		}
	}

	private String toMetaDataText(Identifier identifier) {
		if ( identifier == null ) {
			throw new IllegalArgumentException( "Identifier cannot be null; bad usage" );
		}

		final String text = identifier.getText();
		if ( identifier instanceof DatabaseIdentifier ) {
			return text;
		}
		else if ( identifier.isQuoted() ) {
			return switch ( quotedCaseStrategy ) {
				case UPPER -> text.toUpperCase( Locale.ROOT );
				case LOWER -> text.toLowerCase( Locale.ROOT );
				case MIXED -> text; // default
			};
		}
		else {
			return switch ( unquotedCaseStrategy ) {
				case MIXED -> text;
				case LOWER -> text.toLowerCase( Locale.ROOT );
				case UPPER -> text.toUpperCase( Locale.ROOT ); // default
			};
		}
	}

	@Override
	public String toMetaDataSchemaName(Identifier identifier) {
		if ( !nameQualifierSupport.supportsSchemas() ) {
			// null is used to tell DatabaseMetaData to not limit results based on schema.
			return null;
		}
		else {
			final var id =
					identifier == null
							? jdbcEnvironment.getCurrentSchema()
							: identifier;
			return id == null ? "" : toMetaDataText( id );
		}

	}

	@Override
	public String toMetaDataObjectName(Identifier identifier) {
		if ( identifier == null ) {
			// if this method was called, the value is needed
			throw new IllegalArgumentException( "null was passed as an object name" );
		}
		return toMetaDataText( identifier );
	}
}
