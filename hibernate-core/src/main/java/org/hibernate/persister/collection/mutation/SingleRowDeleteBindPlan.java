/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// Bind plan for a single collection row deletion.
///
/// @author Steve Ebersole
public class SingleRowDeleteBindPlan implements BindPlan {
	private final PersistentCollection<?> collection;
	private final Object key;
	private final Object removal;
	private final CollectionJdbcOperations.Restrictions deleteRowRestrictions;

	public SingleRowDeleteBindPlan(
			PersistentCollection<?> collection,
			Object key,
			Object removal,
			CollectionJdbcOperations.Restrictions deleteRowRestrictions) {
		this.collection = collection;
		this.key = key;
		this.removal = removal;
		this.deleteRowRestrictions = deleteRowRestrictions;
	}

	@Override
	public void bindValues(
			JdbcValueBindings valueBindings,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		deleteRowRestrictions.applyRestrictions( collection, key, removal, -1, session, valueBindings );
	}

	@Override
	public String toString() {
		return "SingleRowDeleteBindPlan(" + collection.getRole() + ")";
	}
}
