/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.spi;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.engine.jdbc.env.internal.NormalizingIdentifierHelperImpl;
import org.jboss.logging.Logger;


/**
 * Builder for {@link IdentifierHelper} instances.  Mainly here to allow progressive
 * building of the immutable (after instantiation) {@link IdentifierHelper}.
 *
 * @author Steve Ebersole
 */
public class IdentifierHelperBuilder {
	private static final Logger LOG = Logger.getLogger( IdentifierHelperBuilder.class );

	private final JdbcEnvironment jdbcEnvironment;

	private NameQualifierSupport nameQualifierSupport = NameQualifierSupport.BOTH;

	//TODO interesting computer science puzzle: find a more compact representation?
	// we only need "contains" on this set, and it has to be case sensitive and efficient.
	private final TreeSet<String> reservedWords = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );

	private boolean globallyQuoteIdentifiers = false;
	private boolean skipGlobalQuotingForColumnDefinitions = false;
	private boolean autoQuoteKeywords = true;
	private boolean autoQuoteInitialUnderscore = false;
	private boolean autoQuoteDollar = false;
	private IdentifierCaseStrategy unquotedCaseStrategy = IdentifierCaseStrategy.UPPER;
	private IdentifierCaseStrategy quotedCaseStrategy = IdentifierCaseStrategy.MIXED;

	public static IdentifierHelperBuilder from(JdbcEnvironment jdbcEnvironment) {
		return new IdentifierHelperBuilder( jdbcEnvironment );
	}

	private IdentifierHelperBuilder(JdbcEnvironment jdbcEnvironment) {
		this.jdbcEnvironment = jdbcEnvironment;
	}

	public boolean isGloballyQuoteIdentifiers() {
		return globallyQuoteIdentifiers;
	}

	public void setGloballyQuoteIdentifiers(boolean globallyQuoteIdentifiers) {
		this.globallyQuoteIdentifiers = globallyQuoteIdentifiers;
	}

	public boolean isSkipGlobalQuotingForColumnDefinitions() {
		return skipGlobalQuotingForColumnDefinitions;
	}

	public void setSkipGlobalQuotingForColumnDefinitions(boolean skipGlobalQuotingForColumnDefinitions) {
		this.skipGlobalQuotingForColumnDefinitions = skipGlobalQuotingForColumnDefinitions;
	}

	public void setAutoQuoteKeywords(boolean autoQuoteKeywords) {
		this.autoQuoteKeywords = autoQuoteKeywords;
	}

	public void setAutoQuoteInitialUnderscore(boolean autoQuoteInitialUnderscore) {
		this.autoQuoteInitialUnderscore = autoQuoteInitialUnderscore;
	}

	public void setAutoQuoteDollar(boolean autoQuoteDollar) {
		this.autoQuoteDollar = autoQuoteDollar;
	}

	public NameQualifierSupport getNameQualifierSupport() {
		return nameQualifierSupport;
	}

	public void setNameQualifierSupport(NameQualifierSupport nameQualifierSupport) {
		this.nameQualifierSupport = nameQualifierSupport == null ? NameQualifierSupport.BOTH : nameQualifierSupport;
	}

	public IdentifierCaseStrategy getUnquotedCaseStrategy() {
		return unquotedCaseStrategy;
	}

	public void setUnquotedCaseStrategy(IdentifierCaseStrategy unquotedCaseStrategy) {
		this.unquotedCaseStrategy = unquotedCaseStrategy;
	}

	public IdentifierCaseStrategy getQuotedCaseStrategy() {
		return quotedCaseStrategy;
	}

	public void setQuotedCaseStrategy(IdentifierCaseStrategy quotedCaseStrategy) {
		this.quotedCaseStrategy = quotedCaseStrategy;
	}

	public void clearReservedWords() {
		this.reservedWords.clear();
	}

	public void applyReservedWords(String... words) {
		applyReservedWords( Arrays.asList( words ) );
	}

	public void applyReservedWords(Collection<String> words) {
		//No use when autoQuoteKeywords is disabled
		if ( autoQuoteKeywords ) {
			reservedWords.addAll( words );
		}
	}

	public void applyReservedWords(Set<String> words) {
		applyReservedWords( (Collection<String>) words );
	}

	public void setReservedWords(Set<String> words) {
		clearReservedWords();
		applyReservedWords( words );
	}

	public IdentifierHelper build() {
		if ( unquotedCaseStrategy == quotedCaseStrategy ) {
			LOG.debugf(
					"IdentifierCaseStrategy for both quoted and unquoted identifiers was set " +
							"to the same strategy [%s]; that will likely lead to problems in schema update " +
							"and validation if using quoted identifiers",
					unquotedCaseStrategy.name()
			);
		}

		return new NormalizingIdentifierHelperImpl(
				jdbcEnvironment,
				nameQualifierSupport,
				globallyQuoteIdentifiers,
				skipGlobalQuotingForColumnDefinitions,
				autoQuoteKeywords,
				autoQuoteInitialUnderscore,
				autoQuoteDollar,
				reservedWords,
				unquotedCaseStrategy,
				quotedCaseStrategy
		);
	}
}
