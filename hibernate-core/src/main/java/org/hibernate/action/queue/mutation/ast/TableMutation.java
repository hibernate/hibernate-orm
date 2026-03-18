/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.jdbc.JdbcOperation;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;

import java.util.List;

/// Table mutation using {@link EntityTableDescriptor} instead of
/// {@link org.hibernate.sql.model.TableMapping}.
///
/// Designed for graph-based ActionQueue where table metadata is pre-built
/// and normalized at SessionFactory initialization.
///
/// Parallel to {@link org.hibernate.sql.model.ast.TableMutation} but works
/// with {@link EntityTableDescriptor} instead of
/// {@link org.hibernate.sql.model.ast.MutatingTableReference}.
///
/// @author Steve Ebersole
@Incubating
public interface TableMutation<O extends JdbcOperation> {
	/// The table being mutated.
	///
	/// This {@link EntityTableDescriptor} is built once at SessionFactory
	/// initialization and contains pre-normalized names and complete
	/// metadata.
	TableDescriptor getTableDescriptor();

	/// The mutation type (INSERT, UPDATE, DELETE)
	MutationType getMutationType();

	/// The mutation target (entity or collection)
	GraphMutationTarget<?> getMutationTarget();

	/// Column value bindings for this mutation
	List<ColumnValueBinding> getValueBindings();

	/// Create the executable JDBC mutation operation.
	///
	/// @return The operation ready for execution
	O createMutationOperation();
}
