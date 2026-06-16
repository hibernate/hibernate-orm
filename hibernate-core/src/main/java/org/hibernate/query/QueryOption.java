/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nullable;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.Statement;
import org.hibernate.CacheMode;
import org.hibernate.Incubating;

import static java.util.Objects.requireNonNull;

/// Declares various [selection query][TypedQuery.Option] and
/// [mutation statement][Statement.Option] options.
///
/// @apiNote Serves no functional purpose - a place to consolidate
///          the actual options for easier discovery / documentation.
///
/// @see CacheMode
/// @see org.hibernate.ReadOnlyMode
///
/// @since 8.0
/// @author Gavin King
@Incubating
public interface QueryOption {

	/// Specifies that the query result set should be cached in the given
	/// region.
	///
	/// This is different to second-level caching of any returned entities
	/// and collections, which is controlled by the {@link CacheMode}.
	///
	/// The query being "eligible" for caching does not necessarily mean
	/// its results will be cached; second-level query caching must also
	/// be explicitly enabled by setting the configuration property
	/// {@value org.hibernate.cfg.CacheSettings#USE_QUERY_CACHE}.
	///
	/// @param region The second-level cache region to use, or `null` for
	///               the default query cache region
	record ResultSetCache(@Nullable String region) implements TypedQuery.Option {
	}

	/// Specifies the JDBC fetch size to use for the query.
	///
	/// @param fetchSize The JDBC fetch size
	///
	/// @see SelectionQuery#setFetchSize(int)
	record JdbcFetchSize(int fetchSize) implements TypedQuery.Option {
	}

	/// Specifies a comment for the SQL query.
	///
	/// If SQL commenting is enabled, the comment will be added to the SQL
	/// query sent to the database, which may be useful for identifying the
	/// source of troublesome queries.
	///
	/// SQL commenting may be enabled using the configuration property
	/// {@value org.hibernate.cfg.JdbcSettings#USE_SQL_COMMENTS}.
	///
	/// @param comment The text of the comment
	record Comment(String comment) implements TypedQuery.Option, Statement.Option {
		public Comment {
			requireNonNull(comment, "Comment text must be specified");
		}
	}
}
