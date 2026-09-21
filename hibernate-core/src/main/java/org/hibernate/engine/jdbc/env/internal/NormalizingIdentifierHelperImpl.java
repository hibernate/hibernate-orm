package org.hibernate.engine.jdbc.env.internal;

import java.util.TreeSet;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.boot.model.naming.DatabaseIdentifier;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.relational.naming.spi.IdentifierComparisonPolicy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;


/**
* @author Steve Ebersole
*/
public class NormalizingIdentifierHelperImpl implements IdentifierHelper {

	private final @Nonnull JdbcEnvironment jdbcEnvironment;

	private final @Nonnull NameQualifierSupport nameQualifierSupport;
	private final boolean globallyQuoteIdentifiers;
	private final boolean globallyQuoteIdentifiersSkipColumnDefinitions;
	private final boolean autoQuoteKeywords;
	private final boolean autoQuoteInitialUnderscore;
	private final boolean autoQuoteDollar;
	private final TreeSet<String> reservedWords;
	private final IdentifierComparisonPolicy comparisonPolicy;
	private final PhysicalName.Factory physicalNameFactory;

	public NormalizingIdentifierHelperImpl(
			@Nonnull JdbcEnvironment jdbcEnvironment,
			@Nonnull NameQualifierSupport nameQualifierSupport,
			boolean globallyQuoteIdentifiers,
			boolean globallyQuoteIdentifiersSkipColumnDefinitions,
			boolean autoQuoteKeywords,
			boolean autoQuoteInitialUnderscore,
			boolean autoQuoteDollar,
			TreeSet<String> reservedWords, //careful, we intentionally omit making a defensive copy to not waste memory
			IdentifierComparisonPolicy comparisonPolicy) {
		this.jdbcEnvironment = jdbcEnvironment;
		this.nameQualifierSupport = nameQualifierSupport;
		this.globallyQuoteIdentifiers = globallyQuoteIdentifiers;
		this.globallyQuoteIdentifiersSkipColumnDefinitions = globallyQuoteIdentifiersSkipColumnDefinitions;
		this.autoQuoteKeywords = autoQuoteKeywords;
		this.autoQuoteInitialUnderscore = autoQuoteInitialUnderscore;
		this.autoQuoteDollar = autoQuoteDollar;
		this.reservedWords = reservedWords;
		this.comparisonPolicy = java.util.Objects.requireNonNull( comparisonPolicy, "comparisonPolicy" );
		this.physicalNameFactory = new PhysicalName.Factory( comparisonPolicy );
	}

	@Override
	public IdentifierComparisonPolicy getComparisonPolicy() {
		return comparisonPolicy;
	}

	@Override
	public PhysicalName.Factory getPhysicalNameFactory() {
		return physicalNameFactory;
	}

	@Override
	@Nullable
	public Identifier normalizeQuoting(@Nullable Identifier identifier) {
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

	private boolean mustQuote(@Nonnull Identifier identifier) {
		final String identifierText = identifier.getText();
		return globallyQuoteIdentifiers
			|| autoQuoteKeywords && isReservedWord( identifierText )
			|| autoQuoteInitialUnderscore && identifierText.startsWith( "_" )
			|| autoQuoteDollar && identifierText.contains( "$" );
	}

	@Override
	@Nullable
	public Identifier toIdentifier(@Nullable String text) {
		return normalizeQuoting( Identifier.toIdentifier( text ) );
	}

	@Override
	@Nullable
	public Identifier toIdentifier(@Nullable String text, boolean quoted) {
		return normalizeQuoting( Identifier.toIdentifier( text, quoted ) );
	}

	@Override
	@Nullable
	public Identifier toIdentifier(@Nullable String text, boolean quoted, boolean isExplicit) {
		return normalizeQuoting( Identifier.toIdentifier( text, quoted, true, isExplicit ) );
	}

	@Override
	@Nullable
	public Identifier applyGlobalQuoting(@Nullable String text) {
		return Identifier.toIdentifier( text, globallyQuoteIdentifiers && !globallyQuoteIdentifiersSkipColumnDefinitions, false );
	}

	@Override
	public boolean isReservedWord(@Nonnull String word) {
		if ( !autoQuoteKeywords ) {
			throw new AssertionFailure( "The reserved keywords map is only initialized if autoQuoteKeywords is true" );
		}
		return reservedWords.contains( word );
	}

	@Override
	@Nullable
	public String toMetaDataCatalogName(@Nullable Identifier identifier) {
		if ( !nameQualifierSupport.supportsCatalogs() ) {
			// null is used to tell DatabaseMetaData to not limit results based on catalog.
			return null;
		}
		else {
			final var id = identifier == null ? jdbcEnvironment.getCurrentCatalog() : identifier;
			return id == null ? "" : toMetaDataText( id );
		}
	}

	@Nonnull
	private String toMetaDataText(@Nonnull Identifier identifier) {
		if ( identifier == null ) {
			throw new IllegalArgumentException( "Identifier cannot be null; bad usage" );
		}

		final String text = identifier.getText();
		if ( identifier instanceof DatabaseIdentifier ) {
			return text;
		}
		else {
			return comparisonPolicy.toDatabaseName( text, identifier.isQuoted() );
		}
	}

	@Override
	@Nullable
	public String toMetaDataSchemaName(@Nullable Identifier identifier) {
		if ( !nameQualifierSupport.supportsSchemas() ) {
			// null is used to tell DatabaseMetaData to not limit results based on schema.
			return null;
		}
		else {
			final var id = identifier == null ? jdbcEnvironment.getCurrentSchema() : identifier;
			return id == null ? "" : toMetaDataText( id );
		}

	}

	@Override
	@Nonnull
	public String toMetaDataObjectName(@Nonnull Identifier identifier) {
		if ( identifier == null ) {
			// if this method was called, the value is needed
			throw new IllegalArgumentException( "null was passed as an object name" );
		}
		return toMetaDataText( identifier );
	}
}
