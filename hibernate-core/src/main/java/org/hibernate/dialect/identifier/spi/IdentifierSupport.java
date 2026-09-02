/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.identifier.spi;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines database identifier quoting, limits, and helper construction.
///
/// Override the smallest operation which represents the database variation.
/// Build an environment-specific helper only from the supplied request, and do
/// not retain its boot-scoped builder or metadata view.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getIdentifierSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface IdentifierSupport {
	/// Return the character which opens a quoted identifier.
	default char openQuote() {
		return '"';
	}

	/// Return the character which closes a quoted identifier.
	default char closeQuote() {
		return '"';
	}

	/// Render a complete quoted identifier, preserving `null`.
	default @Nullable String toQuotedIdentifier(@Nullable String name) {
		return name == null ? null : openQuote() + name + closeQuote();
	}

	/// Convert a correctly paired Hibernate backtick marker to the database's
	/// identifier delimiters. Return every other value unchanged.
	default @Nullable String quote(@Nullable String name) {
		if ( name == null || name.length() < 2 ) {
			return name;
		}
		final int end = name.length() - 1;
		return name.charAt( 0 ) == '`' && name.charAt( end ) == '`'
				? openQuote() + name.substring( 1, end ) + closeQuote()
				: name;
	}

	/// Render a collation name used in a column definition.
	default String quoteCollation(String collation) {
		return collation;
	}

	/// Return the safe maximum length of a generated SQL alias, including room
	/// for Hibernate's generated suffix.
	default int getMaxAliasLength() {
		return 10;
	}

	/// Return the database's maximum identifier length.
	default int getMaxIdentifierLength() {
		return Integer.MAX_VALUE;
	}

	/// Build and supply the configured identifier helper from the stable JDBC
	/// metadata snapshot and selected keyword strategy.
	///
	/// This inherited implementation initializes identifier casing, keywords,
	/// and namespace support from the request. Invoke it before replacing any of
	/// those values with authoritative provider settings, then build the final
	/// helper from the same builder. Settings which this implementation does not
	/// initialize may be applied before invoking it. If builder configuration
	/// cannot express a database rule, decorate the final helper with
	/// [DelegatingIdentifierHelper]. Do not retain the request, builder, or
	/// metadata view.
	///
	/// @since 8.0
	/// @see IdentifierHelper
	@SPI({ USE, IMPLEMENT, SUPPLY })
	default IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		final var jdbcMetadata = request.jdbcMetadata();
		final var keywordSupport = request.keywordSupport();
		builder.setUnquotedCaseStrategy( jdbcMetadata.getUnquotedIdentifierCaseStrategy() );
		builder.setQuotedCaseStrategy( jdbcMetadata.getQuotedIdentifierCaseStrategy() );
		builder.applyReservedWords( keywordSupport.getKeywords() );
		builder.applyReservedWords(
				jdbcMetadata.getSqlKeywords().stream()
						.filter( keywordSupport::acceptsJdbcKeyword )
						.toList()
		);
		builder.setNameQualifierSupport( request.nameQualifierSupport() );
		return builder.build();
	}
}
