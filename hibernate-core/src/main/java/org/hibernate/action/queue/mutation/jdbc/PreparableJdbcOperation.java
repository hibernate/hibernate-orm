/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.jdbc;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.jdbc.Expectation;


/// Graph-based mutation operation that can be prepared as a JDBC statement.
///
/// Parallel to {@link org.hibernate.sql.model.PreparableMutationOperation} but
/// works with {@link EntityTableDescriptor} instead of
/// {@link org.hibernate.sql.model.TableMapping}.
///
/// @author Steve Ebersole
@Incubating
public interface PreparableJdbcOperation extends JdbcOperation {
	/// The SQL string for this operation.
	///
	/// Generated using pre-normalized table and column names from
	/// {@link EntityTableDescriptor}.
	///
	/// @return The SQL string
	String getSqlString();

	/// Whether this is a callable statement (stored procedure/function)
	boolean isCallable();

	/// The expectation for this operation
	Expectation getExpectation();
}
