/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.action.queue.meta.ColumnDescriptor;
import org.hibernate.action.queue.mutation.ast.AssigningTableMutation;
import org.hibernate.action.queue.mutation.jdbc.AssigningJdbcOperation;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;

/// GraphTableMutationBuilder specialization for building mutations which allow
/// assigning values (INSERT or UPDATE).
///
/// @author Steve Ebersole
public interface AssigningGraphTableMutationBuilder<M extends AssigningTableMutation<O>, O extends AssigningJdbcOperation>
		extends GraphTableMutationBuilder<M> {

	/// Add a value binding based on ColumnDescriptor.
	/// This is the preferred method as it uses already-normalized metadata.
	///
	/// @param columnDescriptor The column descriptor.
	void addValueColumn(ColumnDescriptor columnDescriptor);

	/// Add a value binding based on ColumnDescriptor, using a specific assignment value.
	///
	/// This is the preferred method as it uses already-normalized metadata.
	///
	/// @param columnDescriptor The column descriptor.
	/// @param valueExpression A specific value to use as the newly assigned value
	/// 	(as opposed to a parameter marker, generally).
	void addValueColumn(String valueExpression, ColumnDescriptor columnDescriptor);

	/// Add a value binding from SelectableMapping.
	///
	/// Transitional method for compatibility. Normalizes the column name
	/// on the fly. Prefer {@link #addValueColumn(ColumnDescriptor)} when
	/// descriptors are available.
	///
	/// @param selectableMapping The selectable mapping
	void addValueColumn(SelectableMapping selectableMapping);

	/// Add a value binding from SelectableMapping, using a specific assignment value.
	///
	/// Transitional method for compatibility. Normalizes the column name
	/// on the fly. Prefer {@link #addValueColumn(ColumnDescriptor)} when
	/// descriptors are available.
	///
	/// @param selectableMapping The selectable mapping
	/// @param valueExpression A specific value to use as the newly assigned value
	/// 	(as opposed to a parameter marker, generally).
	void addValueColumn(String valueExpression, SelectableMapping selectableMapping);

	/// Add a pre-built column + value binding.
	void addValueColumn(ColumnValueBinding binding);
}
