/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.util.List;

import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.queryhint.spi.QueryHintPlacement;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.spi.SqlComments;

/// Applies database hints and user comments to completed SQL in the order
/// selected by the Dialect.
///
/// @author Steve Ebersole
@Internal
public final class QuerySqlDecorator {
	private QuerySqlDecorator() {
	}

	/// Decorate completed SQL with the database hints and user comment carried
	/// by the query options.
	public static String decorate(
			String sql,
			QueryOptions queryOptions,
			boolean commentsEnabled,
			Dialect dialect) {
		if ( queryOptions == null ) {
			return sql;
		}
		return dialect.getQueryHintPlacement() == QueryHintPlacement.BEFORE_COMMENT
				? addHints( addComment( sql, queryOptions, commentsEnabled ), queryOptions, dialect )
				: addComment( addHints( sql, queryOptions, dialect ), queryOptions, commentsEnabled );
	}

	private static String addHints(String sql, QueryOptions queryOptions, Dialect dialect) {
		final var hints = queryOptions.getDatabaseHints();
		return hints == null || hints.isEmpty()
				? sql
				: dialect.getQueryHintString( sql, List.copyOf( hints ) );
	}

	private static String addComment(String sql, QueryOptions queryOptions, boolean commentsEnabled) {
		final String comment = queryOptions.getComment();
		return commentsEnabled && comment != null
				? "/* " + SqlComments.escape( comment ) + " */ " + sql
				: sql;
	}
}
