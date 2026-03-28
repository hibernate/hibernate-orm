/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.ArrayList;
import java.util.List;

/// Bind plan which handles all removals for a collection applying multiple row-level
/// deletions as part of its execution.
///
/// @author Steve Ebersole
public class BundledCollectionDeleteBindPlan implements BindPlan {
	private final PersistentCollection<?> collection;
	private final Object key;
	private final CollectionJdbcOperations.Restrictions restrictions;
	private final List<Object> entries;

	public BundledCollectionDeleteBindPlan(
			PersistentCollection<?> collection,
			Object key,
			CollectionJdbcOperations.Restrictions restrictions,
			List<Object> entries) {
		this.collection = collection;
		this.key = key;
		this.restrictions = restrictions;
		this.entries = new ArrayList<>(entries);
	}

	@Override
	public void execute(
			org.hibernate.action.queue.exec.ExecutionContext context,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		// Drive execution by calling context.executeRow() for each entry
		for (final Object removal : entries) {
			context.executeRow(
					plannedOperation,
					(valueBindings, s) -> bindEntry(removal, valueBindings, session),
					null
			);
		}
	}

	private void bindEntry(Object removal, JdbcValueBindings valueBindings, SharedSessionContractImplementor session) {
		restrictions.applyRestrictions( collection, key, removal, -1, session, valueBindings );
	}
}
