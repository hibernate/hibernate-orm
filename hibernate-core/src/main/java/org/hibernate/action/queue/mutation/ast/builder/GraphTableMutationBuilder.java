/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.ast.TableMutation;
import org.hibernate.sql.ast.tree.from.TableReference;

/// Builder for graph-based table mutations.
///
/// Accepts {@link EntityTableDescriptor} instead of
/// {@link org.hibernate.sql.model.TableMapping}, allowing builders to work
/// with pre-normalized, cached metadata.
///
/// Parallel to {@link org.hibernate.sql.model.ast.builder.TableMutationBuilder}
/// but designed for graph-based ActionQueue.
///
/// @author Steve Ebersole
@Incubating
public interface GraphTableMutationBuilder<M extends TableMutation<?>> {
	/// Get the table descriptor being mutated.
	///
	/// The descriptor contains pre-normalized table and column names,
	/// mutation details (custom SQL, expectations), and key information.
	TableDescriptor getTableDescriptor();

	TableReference getTableReference();

	/// Build the mutation.
	///
	/// @return The built mutation ready to create operations
	M buildMutation();
}
