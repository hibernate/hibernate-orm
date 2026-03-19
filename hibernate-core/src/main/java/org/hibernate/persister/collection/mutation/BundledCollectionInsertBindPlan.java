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
 * Bundled bind plan for collection insert operations that handles multiple collection entries
 * in a single PlannedOperation.
 *
 * @author Steve Ebersole
 */
public class BundledCollectionInsertBindPlan implements BindPlan {
	private final CollectionJdbcOperations.Values values;
	private final PersistentCollection<?> collection;
	private final Object key;
	private final List<BundledBindPlanEntry> entries;

	public BundledCollectionInsertBindPlan(
			CollectionJdbcOperations.Values values,
			PersistentCollection<?> collection,
			Object key,
			List<Object> entries,
			List<Integer> entryIndices) {
		this.values = values;
		this.collection = collection;
		this.key = key;
		this.entries = new ArrayList<>(entries.size());

		for (int i = 0; i < entries.size(); i++) {
			this.entries.add(new BundledBindPlanEntry(entries.get(i), entryIndices.get(i)));
		}
	}

	public BundledCollectionInsertBindPlan(
			CollectionJdbcOperations.Values values,
			PersistentCollection<?> collection,
			Object key,
			List<BundledBindPlanEntry> entries) {
		this.values = values;
		this.collection = collection;
		this.key = key;
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
		values.applyValues( collection, key, entryData.entry(), entryData.entryIndex(), session, valueBindings );
	}
}
