/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.ColumnDescriptor;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableKeyDescriptor;
import org.hibernate.action.queue.mutation.ast.TableDelete;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;

import java.util.List;

/// Builder for graph-based table DELETE mutations.
///
/// Parallel to {@link org.hibernate.sql.model.ast.builder.TableDeleteBuilder}
/// but works with {@link EntityTableDescriptor}.
///
/// @author Steve Ebersole
@Incubating
public interface GraphTableDeleteBuilder extends GraphTableMutationBuilder<TableDelete> {
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

	List<ColumnValueBinding> getOptimisticLockBindings();

	List<ColumnValueBinding> getKeyRestrictionBindings();

}
