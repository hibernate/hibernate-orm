/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.spi;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.engine.jdbc.env.internal.NormalizingIdentifierHelperImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;

import org.jboss.logging.Logger;

/**
 * Builder for IdentifierHelper instances.  Mainly here to allow progressive
 * building of the immutable (after instantiation) IdentifierHelper.
 *
 * @author Steve Ebersole
 */
public class IdentifierHelperBuilder {
	private static final Logger log = Logger.getLogger( IdentifierHelperBuilder.class );

	private final JdbcEnvironment jdbcEnvironment;

	private NameQualifierSupport nameQualifierSupport = NameQualifierSupport.BOTH;

	private Set<String> reservedWords = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
	private boolean globallyQuoteIdentifiers = false;
	private boolean skipGlobalQuotingForColumnDefinitions = false;
	private boolean autoQuoteKeywords = true;
	private IdentifierCaseStrategy unquotedCaseStrategy = IdentifierCaseStrategy.MIXED;
	private IdentifierCaseStrategy quotedCaseStrategy = IdentifierCaseStrategy.MIXED;

	public static IdentifierHelperBuilder from(JdbcEnvironment jdbcEnvironment) {
		return new IdentifierHelperBuilder( jdbcEnvironment );
	}

	private IdentifierHelperBuilder(JdbcEnvironment jdbcEnvironment) {
		this.jdbcEnvironment = jdbcEnvironment;
	}

	/**
	 * Applies any reserved words reported via {@link DatabaseMetaData#getSQLKeywords()}
	 *
	 * @param metaData The metadata to get reserved words from
	 *
	 * @throws SQLException Any access to DatabaseMetaData can case SQLException; just re-throw.
	 */
	public void applyReservedWords(DatabaseMetaData metaData) throws SQLException {
		if ( metaData == null ) {
			return;
		}

		this.reservedWords.addAll( parseKeywords( metaData.getSQLKeywords() ) );
	}

	private static List<String> parseKeywords(String extraKeywordsString) {
		return StringHelper.parseCommaSeparatedString( extraKeywordsString );
	}

	public void applyIdentifierCasing(DatabaseMetaData metaData) throws SQLException {
		if ( metaData == null ) {
			return;
		}

		final int unquotedAffirmatives = ArrayHelper.countTrue(
				metaData.storesLowerCaseIdentifiers(),
				metaData.storesUpperCaseIdentifiers(),
				metaData.storesMixedCaseIdentifiers()
		);

		if ( unquotedAffirmatives == 0 ) {
			log.debug( "JDBC driver metadata reported database stores unquoted identifiers in neither upper, lower nor mixed case" );
		}
		else {
			// NOTE : still "dodgy" if more than one is true
			if ( unquotedAffirmatives > 1 ) {
				log.debug( "JDBC driver metadata reported database stores unquoted identifiers in more than one case" );
			}

			if ( metaData.storesUpperCaseIdentifiers() ) {
				this.unquotedCaseStrategy = IdentifierCaseStrategy.UPPER;
			}
			else if ( metaData.storesLowerCaseIdentifiers() ) {
				this.unquotedCaseStrategy = IdentifierCaseStrategy.LOWER;
			}
			else {
				this.unquotedCaseStrategy = IdentifierCaseStrategy.MIXED;
			}
		}


		final int quotedAffirmatives = ArrayHelper.countTrue(
				metaData.storesLowerCaseQuotedIdentifiers(),
				metaData.storesUpperCaseQuotedIdentifiers(),
				metaData.storesMixedCaseQuotedIdentifiers()
		);

		if ( quotedAffirmatives == 0 ) {
			log.debug( "JDBC driver metadata reported database stores quoted identifiers in neither upper, lower nor mixed case" );
		}
		else {
			// NOTE : still "dodgy" if more than one is true
			if ( quotedAffirmatives > 1 ) {
				log.debug( "JDBC driver metadata reported database stores quoted identifiers in more than one case" );
			}

			if ( metaData.storesMixedCaseQuotedIdentifiers() ) {
				this.quotedCaseStrategy = IdentifierCaseStrategy.MIXED;
			}
			else if ( metaData.storesLowerCaseQuotedIdentifiers() ) {
				this.quotedCaseStrategy = IdentifierCaseStrategy.LOWER;
			}
			else {
				this.quotedCaseStrategy = IdentifierCaseStrategy.UPPER;
			}
		}
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

	public void applyReservedWords(Set<String> words) {
		this.reservedWords.addAll( words );
	}

	public void setReservedWords(Set<String> words) {
		clearReservedWords();
		applyReservedWords( words );
	}

	public IdentifierHelper build() {
		if ( unquotedCaseStrategy == quotedCaseStrategy ) {
			log.debugf(
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
				reservedWords,
				unquotedCaseStrategy,
				quotedCaseStrategy
		);
	}
}
