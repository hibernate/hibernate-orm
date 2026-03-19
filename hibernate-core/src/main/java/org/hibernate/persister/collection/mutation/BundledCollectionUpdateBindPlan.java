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

import java.util.ArrayList;
import java.util.List;

/**
 * Bundled bind plan for collection update operations that handles multiple collection entries
 * in a single PlannedOperation.
 *
 * @author Steve Ebersole
 */
public class BundledCollectionUpdateBindPlan implements BindPlan {
	private final PersistentCollection<?> collection;
	private final Object key;
	private final CollectionJdbcOperations.Values values;
	private final CollectionJdbcOperations.Restrictions restrictions;
	private final List<BundledBindPlanEntry> entries;

	public BundledCollectionUpdateBindPlan(
			PersistentCollection<?> collection,
			Object key,
			CollectionJdbcOperations.Values values,
			CollectionJdbcOperations.Restrictions restrictions,
			List<Object> entries,
			List<Integer> entryIndices) {
		this.collection = collection;
		this.key = key;
		this.values = values;
		this.restrictions = restrictions;
		this.entries = new ArrayList<>(entries.size());

		for (int i = 0; i < entries.size(); i++) {
			this.entries.add(new BundledBindPlanEntry(entries.get(i), entryIndices.get(i)));
		}
	}

	public BundledCollectionUpdateBindPlan(
			PersistentCollection<?> collection,
			Object key,
			CollectionJdbcOperations.Values values,
			CollectionJdbcOperations.Restrictions restrictions,
			List<BundledBindPlanEntry> entries) {
		this.collection = collection;
		this.key = key;
		this.values = values;
		this.restrictions = restrictions;
		this.entries = new ArrayList<>(entries);
	}

	@Override
	public void execute(
			org.hibernate.action.queue.exec.ExecutionContext context,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		// Drive execution by calling context.executeRow() for each entry
		for (final BundledBindPlanEntry entryData : entries) {
			context.executeRow(
					plannedOperation,
					valueBindings -> bindEntry(entryData, valueBindings, session),
					null
			);
		}
	}

	private void bindEntry(BundledBindPlanEntry entryData, JdbcValueBindings valueBindings, SharedSessionContractImplementor session) {
		// Bind value columns (SET clause)
		values.applyValues( collection, key, entryData.entry(), entryData.entryIndex(), session, valueBindings );

		// Bind restriction columns (WHERE clause)
		restrictions.applyRestrictions( collection, key, entryData.entry(), entryData.entryIndex(), session, valueBindings );
	}
}
