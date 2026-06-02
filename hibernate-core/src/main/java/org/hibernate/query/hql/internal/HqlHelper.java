/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.internal;

import org.antlr.v4.runtime.Token;

import jakarta.annotation.Nullable;

import static org.hibernate.grammars.hql.HqlLexer.FILTER;
import static org.hibernate.grammars.hql.HqlLexer.FROM;
import static org.hibernate.grammars.hql.HqlLexer.GROUP;
import static org.hibernate.grammars.hql.HqlLexer.HAVING;
import static org.hibernate.grammars.hql.HqlLexer.LEFT_PAREN;
import static org.hibernate.grammars.hql.HqlLexer.ORDER;
import static org.hibernate.grammars.hql.HqlLexer.WHERE;

/**
 * Utility methods for HQL string manipulation.
 * In particular shared with the annotation processor.
 */
public final class HqlHelper {

	private HqlHelper() {
	}

	/**
	 * If the given HQL string has no {@code from} clause, insert
	 * {@code from entityName} before the first {@code where},
	 * {@code having}, {@code group}, or {@code order} keyword.
	 * If none of those keywords exist, append it at the end.
	 *
	 * @param hql the (possibly partial) HQL query string
	 * @param entityName the entity name to insert, or {@code null} to skip
	 * @return the HQL string with a {@code from} clause
	 */
	public static String addFromClauseIfNecessary(String hql, @Nullable String entityName) {
		if ( entityName == null ) {
			return hql;
		}
		else if ( hql.isBlank() ) {
			return "from " + entityName;
		}
		else if ( isMutationStatement( hql ) ) {
			return hql;
		}
		else {
			final var hqlLexer = HqlParseTreeBuilder.INSTANCE.buildHqlLexer( hql );
			final var allTokens = hqlLexer.getAllTokens();
			int previousType = -1;
			int previousPreviousType = -1;
			for ( final var token : allTokens ) {
				if ( token.getChannel() == Token.DEFAULT_CHANNEL ) {
					final int tokenType = token.getType();
					switch ( tokenType ) {
						case FROM:
							return hql;
						case WHERE:
							if ( previousType == LEFT_PAREN && previousPreviousType == FILTER ) {
								break;
							}
							// fall through
						case HAVING:
						case GROUP:
						case ORDER:
							return new StringBuilder( hql )
									.insert( token.getStartIndex(), "from " + entityName + " " )
									.toString();
					}
					previousPreviousType = previousType;
					previousType = tokenType;
				}
			}
			return hql + " from " + entityName;
		}
	}

	/**
	 * Does the given HQL string begin with an {@code insert},
	 * {@code update}, or {@code delete} keyword?
	 */
	public static boolean isMutationStatement(String hql) {
		final String trimmed = hql.trim();
		final String keyword = trimmed.length() > 6 ? trimmed.substring( 0, 6 ) : "";
		return keyword.equalsIgnoreCase( "update" )
			|| keyword.equalsIgnoreCase( "delete" )
			|| keyword.equalsIgnoreCase( "insert" );
	}
}
