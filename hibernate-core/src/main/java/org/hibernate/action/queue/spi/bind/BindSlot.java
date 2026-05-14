/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.bind;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/// A single JDBC parameter slot in a mutation statement.
///
/// Each slot corresponds to one [org.hibernate.sql.model.ast.ColumnValueParameter]
/// in the mutation operation's ordered parameter list.  The slot keeps the
/// stable array index used by [JdbcValueBindings], the JDBC parameter position
/// used when binding the statement, and the descriptor information needed by
/// compatibility callers that still expect descriptor-based bindings.
///
/// @param index the zero-based position of this slot within the owning
/// [MutationBindTemplate#slots()] array
/// @param columnName the physical column expression represented by the
/// underlying column-value parameter
/// @param usage whether the parameter is used as an assignment value or as a
/// restriction predicate value
/// @param jdbcPosition the one-based JDBC parameter position in the prepared
/// statement, including any leading expectation parameters
/// @param jdbcMapping the mapping used to bind the Java value to JDBC
/// @param valueDescriptor descriptor view of this slot used by compatibility
/// binding APIs
///
/// @author Steve Ebersole
public record BindSlot(
		int index,
		String columnName,
		ParameterUsage usage,
		int jdbcPosition,
		JdbcMapping jdbcMapping,
		JdbcValueDescriptor valueDescriptor) {
}
