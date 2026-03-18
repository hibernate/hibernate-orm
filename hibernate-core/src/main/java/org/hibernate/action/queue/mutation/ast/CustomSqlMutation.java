/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast;

import org.hibernate.Incubating;

/// Marker interface for graph-based mutations that use custom SQL.
///
/// Parallel to {@link org.hibernate.sql.model.ast.CustomSqlMutation}.
///
/// @author Steve Ebersole
@Incubating
public interface CustomSqlMutation {
	/// The custom SQL string
	String getCustomSql();

	/// Whether the custom SQL is callable (stored procedure/function)
	boolean isCallable();
}
