/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.internal;

import java.util.Locale;

import org.hibernate.relational.naming.spi.IdentifierComparisonPolicy;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;

import static java.util.Objects.requireNonNull;

/// Default policy based on the effective quoted and unquoted storage-case rules.
/// Databases whose comparison differs from storage casing require a provider policy.
///
/// @author Steve Ebersole
public record CaseBasedIdentifierComparisonPolicy(
		IdentifierCaseStrategy unquotedCaseStrategy,
		IdentifierCaseStrategy quotedCaseStrategy) implements IdentifierComparisonPolicy {
	public CaseBasedIdentifierComparisonPolicy {
		requireNonNull( unquotedCaseStrategy, "unquotedCaseStrategy" );
		requireNonNull( quotedCaseStrategy, "quotedCaseStrategy" );
	}

	@Override
	public String toDatabaseName(String text, boolean quoted) {
		requireNonNull( text, "text" );
		return switch ( quoted ? quotedCaseStrategy : unquotedCaseStrategy ) {
			case UPPER -> text.toUpperCase( Locale.ROOT );
			case LOWER -> text.toLowerCase( Locale.ROOT );
			case MIXED -> text;
		};
	}
}
