/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import org.hibernate.sql.model.MutationOperation;

import java.util.List;
import java.util.function.BiConsumer;

/// Marker interface for TableMutations which assign values - INSERT, UPDATE, MERGE.
///
/// @author Steve Ebersole
public interface AssigningTableMutation<O extends MutationOperation> extends TableMutation<O> {
	/// The number of [value bindings][#getValueBindings].
	///
	/// @see #getValueBindings()
	default int getNumberOfValueBindings() {
		return getValueBindings().size();
	}

	/// The value bindings for each column.
	///
	/// @implNote Table key column(s) are not included here as
	/// those are not ever updated
	List<ColumnValueBinding> getValueBindings();

	/// Visit each [value binding][#getValueBindings]
	void forEachValueBinding(BiConsumer<Integer, ColumnValueBinding> consumer);
}
