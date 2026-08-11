/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;

import org.hibernate.action.queue.spi.bind.GroupedRowBindPlan;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.decompose.collection.CollectionJdbcOperations;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.collection.spi.FrozenCollectionRows;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/// Compact binding plan for collection-row inserts which share one operation
/// template and graph placement.
///
/// @author Steve Ebersole
/// @since 8.0
final class GroupedCollectionInsertBindPlan implements GroupedRowBindPlan {
	private final CollectionPersister persister;
	private final CollectionJdbcOperations.Values values;
	private final PersistentCollection<?> collection;
	private final Object key;
	private final FrozenCollectionRows rows;

	GroupedCollectionInsertBindPlan(
			CollectionPersister persister,
			CollectionJdbcOperations.Values values,
			PersistentCollection<?> collection,
			Object key,
			FrozenCollectionRows rows) {
		this.persister = persister;
		this.values = values;
		this.collection = collection;
		this.key = key;
		this.rows = rows;
	}

	@Override
	public int getBindingCount() {
		return rows.size();
	}

	@Override
	public void bindValues(
			int bindingIndex,
			JdbcValueBindings jdbcValueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + persister.getNavigableRole().getFullPath() );
		}
		values.applyValues(
				collection,
				key,
				rows.entry( bindingIndex ),
				rows.position( bindingIndex ),
				session,
				jdbcValueBindings
		);
	}
}
