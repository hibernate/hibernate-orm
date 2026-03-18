/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.ColumnDescriptor;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableKeyDescriptor;
import org.hibernate.action.queue.mutation.ast.TableUpdate;
import org.hibernate.action.queue.mutation.jdbc.JdbcUpdate;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;

import java.util.List;

/// Builder for graph-based table UPDATE mutations.
///
/// Parallel to {@link org.hibernate.sql.model.ast.builder.TableUpdateBuilder}
/// but works with {@link EntityTableDescriptor}.
///
/// @author Steve Ebersole
@Incubating
public interface GraphTableUpdateBuilder extends AssigningGraphTableMutationBuilder<TableUpdate, JdbcUpdate> {

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

	/// Add key restrictions using pre-built TableKeyDescriptor.
	///
	/// @param keyDescriptor The key descriptor with normalized column names
	void addKeyRestrictions(TableKeyDescriptor keyDescriptor);

	/// Add a single key restriction column.
	///
	/// @param columnDescriptor The key column descriptor
	void addKeyRestriction(ColumnDescriptor columnDescriptor);

	/// Add a key restriction from SelectableMapping.
	///
	/// Transitional method for compatibility.
	///
	/// @param selectableMapping The selectable mapping
	void addKeyRestriction(SelectableMapping selectableMapping);

	/// Add an optimistic lock restriction.
	///
	/// @param columnDescriptor The version/lock column descriptor
	void addOptimisticLockRestriction(ColumnDescriptor columnDescriptor);

	/// Add an optimistic lock restriction from SelectableMapping.
	///
	/// Transitional method for compatibility.
	///
	/// @param selectableMapping The selectable mapping
	void addOptimisticLockRestriction(SelectableMapping selectableMapping);

	void addOptimisticLockRestriction(Object value, SelectableMapping jdbcValueMapping);

	void addNonKeyRestriction(ColumnValueBinding binding);

	List<ColumnValueBinding> getOptimisticLockBindings();

	List<ColumnValueBinding> getKeyRestrictionBindings();

	/// Check if this builder has any value bindings (SET clause columns).
	///
	/// @return true if there are columns to update, false otherwise
	boolean hasValueBindings();
}
