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
import org.hibernate.persister.collection.CollectionPersister;

/// Bind plan for a single collection row insert.
///
/// @author Steve Ebersole
public class SingleRowInsertBindPlan implements BindPlan {
	private final CollectionPersister persister;
	private final CollectionJdbcOperations.Values values;

	private final PersistentCollection<?> collection;
	private final Object key;
	private final Object entry;
	private final int entryIndex;

	public SingleRowInsertBindPlan(
			CollectionPersister persister,
			CollectionJdbcOperations.Values values,
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryIndex) {
		this.persister = persister;
		this.values = values;
		this.collection = collection;
		this.key = key;
		this.entry = entry;
		this.entryIndex = entryIndex;
	}

	@Override
	public void bindValues(
			JdbcValueBindings jdbcValueBindings,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + persister.getNavigableRole().getFullPath() );
		}

		values.applyValues( collection, key, entry, entryIndex, session, jdbcValueBindings );
	}
}
