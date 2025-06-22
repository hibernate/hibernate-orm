/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractRestrictedTableMutation<O extends MutationOperation>
		extends AbstractTableMutation<O>
		implements RestrictedTableMutation<O> {
	private final List<ColumnValueBinding> keyRestrictionBindings;
	private final List<ColumnValueBinding> optLockRestrictionBindings;

	public AbstractRestrictedTableMutation(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			String comment,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		super( mutatingTable, mutationTarget, comment, parameters );
		this.keyRestrictionBindings = keyRestrictionBindings;
		this.optLockRestrictionBindings = optLockRestrictionBindings;
	}

	@Override
	public List<ColumnValueBinding> getKeyBindings() {
		return keyRestrictionBindings;
	}

	@Override
	public void forEachKeyBinding(BiConsumer<Integer, ColumnValueBinding> consumer) {
		forEachThing( keyRestrictionBindings, consumer );
	}

	@Override
	public List<ColumnValueBinding> getOptimisticLockBindings() {
		return optLockRestrictionBindings;
	}

	@Override
	public void forEachOptimisticLockBinding(BiConsumer<Integer, ColumnValueBinding> consumer) {
		forEachThing( optLockRestrictionBindings, consumer );
	}

	@Override
	public void forEachValueBinding(BiConsumer<Integer, ColumnValueBinding> consumer) {
	}
}
