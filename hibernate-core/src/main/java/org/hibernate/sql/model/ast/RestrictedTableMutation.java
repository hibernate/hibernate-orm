/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.ast;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.sql.model.MutationOperation;

/**
 * Specialized TableMutation implementation for mutations which
 * define a where-clause
 *
 * @author Steve Ebersole
 */
public interface RestrictedTableMutation<O extends MutationOperation>
		extends TableMutation<O> {
	List<ColumnValueBinding> getKeyBindings();

	default int getNumberOfKeyBindings() {
		return getKeyBindings().size();
	}

	void forEachKeyBinding(BiConsumer<Integer,ColumnValueBinding> consumer);

	List<ColumnValueBinding> getOptimisticLockBindings();

	default int getNumberOfOptimisticLockBindings() {
		final List<ColumnValueBinding> bindings = getOptimisticLockBindings();
		return bindings == null ? 0 : bindings.size();
	}

	void forEachOptimisticLockBinding(BiConsumer<Integer,ColumnValueBinding> consumer);
}
