/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import org.hibernate.action.queue.constraint.UniqueConstraint;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.bind.OperationResultChecker;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import java.sql.SQLException;

/// Bind plan for a single collection row update.
///
/// @author Steve Ebersole
public class SingleRowUpdateBindPlan implements BindPlan, OperationResultChecker {
	private final CollectionPersister persister;
	private final PersistentCollection<?> collection;
	private final Object key;
	private final Object entry;
	private final int entryIndex;
	private final CollectionJdbcOperations.Values updateRowValues;
	private final CollectionJdbcOperations.Restrictions updateRowRestrictions;

	public SingleRowUpdateBindPlan(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryIndex,
			CollectionJdbcOperations.Values updateRowValues,
			CollectionJdbcOperations.Restrictions updateRowRestrictions) {
		this.persister = persister;
		this.collection = collection;
		this.key = key;
		this.entry = entry;
		this.entryIndex = entryIndex;
		this.updateRowValues = updateRowValues;
		this.updateRowRestrictions = updateRowRestrictions;
	}

	@Override
	public Object getEntityInstance() {
		// Collection operations don't represent entity operations - they represent
		// the collection relationship (FK updates for one-to-many, join table rows for many-to-many).
		// Returning the element creates artificial dependencies that fragment batches.
		return null;
	}

	@Override
	public Object[] getUniqueConstraintValues(
			UniqueConstraint constraint,
			SharedSessionContractImplementor session) {
		return CollectionUniqueKeyValueExtractor.extractValues(
				persister,
				collection,
				key,
				entry,
				entryIndex,
				constraint,
				session
		);
	}

	@Override
	public void bindValues(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		updateRowValues.applyValues( collection, key, entry, entryIndex, session, valueBindings );
		updateRowRestrictions.applyRestrictions( collection, key, entry, entryIndex, session, valueBindings );
	}

	@Override
	public boolean checkResult(
			int affectedRowCount,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException {
		// something we should check here?
		return true;
	}

	@Override
	public String toString() {
		return "SingleRowUpdateBindPlan(" + collection.getRole() + ")";
	}
}
