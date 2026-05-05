/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;

import org.hibernate.action.queue.internal.constraint.UniqueConstraint;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.decompose.collection.CollectionJdbcOperations;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import java.sql.SQLException;

/// Bind plan for updating only the order/index column of a collection row.
///
/// Used when an element's position changes but the element itself remains in the
/// collection.  The plan exposes both old and new unique-slot values so the
/// graph planner can order an index move before an insert that wants the same
/// collection slot.
///
/// @author Steve Ebersole
public class OrderOnlyUpdateBindPlan implements BindPlan, OperationResultChecker {
	private final CollectionPersister persister;
	private final PersistentCollection<?> collection;
	private final Object key;
	private final Object entry;
	private final int oldPosition;
	private final int newPosition;
	private final CollectionJdbcOperations.Values updateRowValues;
	private final CollectionJdbcOperations.Restrictions updateRowRestrictions;

	public OrderOnlyUpdateBindPlan(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int oldPosition,
			int newPosition,
			CollectionJdbcOperations.Values updateRowValues,
			CollectionJdbcOperations.Restrictions updateRowRestrictions) {
		this.persister = persister;
		this.collection = collection;
		this.key = key;
		this.entry = entry;
		this.oldPosition = oldPosition;
		this.newPosition = newPosition;
		this.updateRowValues = updateRowValues;
		this.updateRowRestrictions = updateRowRestrictions;
	}

	@Override
	public Object getEntityInstance() {
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
				newPosition,
				constraint,
				session
		);
	}

	@Override
	public Object[] getPreviousUniqueConstraintValues(
			UniqueConstraint constraint,
			SharedSessionContractImplementor session) {
		return CollectionUniqueKeyValueExtractor.extractValues(
				persister,
				collection,
				key,
				entry,
				oldPosition,
				constraint,
				session
		);
	}

	@Override
	public void bindValues(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		updateRowValues.applyValues( collection, key, entry, newPosition, session, valueBindings );
		updateRowRestrictions.applyRestrictions( collection, key, entry, oldPosition, session, valueBindings );
	}

	@Override
	public boolean checkResult(
			int affectedRowCount,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException {
		return true;
	}

	@Override
	public String toString() {
		return "OrderOnlyUpdateBindPlan(" + collection.getRole() + ", " + oldPosition + "->" + newPosition + ")";
	}
}
