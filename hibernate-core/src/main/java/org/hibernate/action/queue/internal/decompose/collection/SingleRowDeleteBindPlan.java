/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;

import org.hibernate.action.queue.spi.decompose.collection.CollectionJdbcOperations;

import org.hibernate.action.queue.internal.constraint.UniqueConstraint;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import java.sql.SQLException;

/// Bind plan for a single collection row deletion.
///
/// @author Steve Ebersole
public class SingleRowDeleteBindPlan implements BindPlan, OperationResultChecker {
	private final CollectionPersister persister;
	private final PersistentCollection<?> collection;
	private final Object key;
	private final Object removal;
	private final CollectionJdbcOperations.Restrictions deleteRowRestrictions;

	public SingleRowDeleteBindPlan(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			Object key,
			Object removal,
			CollectionJdbcOperations.Restrictions deleteRowRestrictions) {
		this.persister = persister;
		this.collection = collection;
		this.key = key;
		this.removal = removal;
		this.deleteRowRestrictions = deleteRowRestrictions;
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
				removal,
				-1,
				constraint,
				session
		);
	}

	@Override
	public void bindValues(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		deleteRowRestrictions.applyRestrictions( collection, key, removal, -1, session, valueBindings );
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
		return "SingleRowDeleteBindPlan(" + collection.getRole() + ")";
	}
}
