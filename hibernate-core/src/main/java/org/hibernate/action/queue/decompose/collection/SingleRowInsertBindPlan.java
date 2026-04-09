/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import org.hibernate.action.queue.exec.BindPlan;
import org.hibernate.action.queue.exec.JdbcValueBindings;
import org.hibernate.action.queue.exec.ExecutionContext;
import org.hibernate.action.queue.exec.OperationResultChecker;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import java.sql.SQLException;

/// Bind plan for a single collection row insert.
///
/// @author Steve Ebersole
public class SingleRowInsertBindPlan implements BindPlan, OperationResultChecker {
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
	public Object getEntityInstance() {
		// Collection operations don't represent entity operations - they represent
		// the collection relationship (FK updates for one-to-many, join table rows for many-to-many).
		// Returning the element creates artificial dependencies that fragment batches.
		return null;
	}

	@Override
	public void execute(
			ExecutionContext context,
			PlannedOperation plannedOperation,
			SharedSessionContractImplementor session) {
		context.executeRow( plannedOperation, this::bindValues, this );
	}

	private void bindValues(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + persister.getNavigableRole().getFullPath() );
		}

		values.applyValues( collection, key, entry, entryIndex, session, jdbcValueBindings );
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
}
