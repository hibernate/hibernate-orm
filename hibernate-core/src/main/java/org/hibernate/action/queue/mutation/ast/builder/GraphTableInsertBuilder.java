/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.ColumnDescriptor;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.mutation.ast.TableInsert;
import org.hibernate.action.queue.mutation.jdbc.JdbcInsert;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;

/// Builder for graph-based table INSERT mutations.
///
/// Parallel to {@link org.hibernate.sql.model.ast.builder.TableInsertBuilder}
/// but works with {@link EntityTableDescriptor}.
///
/// @author Steve Ebersole
@Incubating
public interface GraphTableInsertBuilder extends AssigningGraphTableMutationBuilder<TableInsert, JdbcInsert> {

	/// Add a value column binding using pre-built ColumnDescriptor.
	///
	/// This is the preferred method as it uses already-normalized metadata.
	///
	/// @param columnDescriptor The column descriptor with normalized names
	void addValueColumn(ColumnDescriptor columnDescriptor);

	void addValueColumn(String valueExpression, ColumnDescriptor columnDescriptor);

	/// Add a value column binding from SelectableMapping.
	///
	/// Transitional method for compatibility. Normalizes the column name
	/// on the fly. Prefer {@link #addValueColumn(ColumnDescriptor)} when
	/// descriptors are available.
	///
	/// @param selectableMapping The selectable mapping
	void addValueColumn(SelectableMapping selectableMapping);

	void addValueColumn(String valueExpression, SelectableMapping selectableMapping);

	void addValueColumn(ColumnValueBinding binding);

	/// Add a key column binding using ColumnDescriptor.
	///
	/// @param columnDescriptor The key column descriptor
	void addKeyColumn(ColumnDescriptor columnDescriptor);

	/// Add a key column binding from SelectableMapping.
	///
	/// Transitional method for compatibility.
	///
	/// @param selectableMapping The selectable mapping
	void addKeyColumn(SelectableMapping selectableMapping);

	/// Add a key column with custom expression.
	///
	/// Used for special cases like identity column generation.
	///
	/// @param columnWriteFragment The custom SQL fragment
	/// @param columnDescriptor The column descriptor
	void addKeyColumn(String columnWriteFragment, ColumnDescriptor columnDescriptor);
}
