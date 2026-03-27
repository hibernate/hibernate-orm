/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.MutationOperation;

/// Manages standard MutationOperation, as well as parameter binding details, for collection mutations.
///
/// Built by collection persisters for use in decomposers.
///
/// @author Steve Ebersole
public class CollectionJdbcOperations {
	private final CollectionGraphMutationTarget target;

	private final InsertRowPlan insertRowPlan;
	private final UpdateRowPlan updateRowPlan;
	private final DeleteRowPlan deleteRowPlan;

	private final MutationOperation removeOperation;

	public CollectionJdbcOperations(
			CollectionGraphMutationTarget target,
			MutationOperation insertRowOperation,
			Values insertRowValues,
			MutationOperation updateRowOperation,
			Values updateRowValues,
			Restrictions updateRowRestrictions,
			MutationOperation deleteRowOperation,
			Restrictions deleteRowRestrictions,
			MutationOperation removeOperation) {
		this(
				target,
				new InsertRowPlan( insertRowOperation, insertRowValues ),
				new UpdateRowPlan( updateRowOperation, updateRowValues, updateRowRestrictions ),
				new DeleteRowPlan( deleteRowOperation, deleteRowRestrictions ),
				removeOperation
		);
	}

	public CollectionJdbcOperations(
			CollectionGraphMutationTarget target,
			InsertRowPlan insertRowPlan,
			UpdateRowPlan updateRowPlan,
			DeleteRowPlan deleteRowPlan,
			MutationOperation removeOperation) {
		this.target = target;
		this.insertRowPlan = insertRowPlan;
		this.updateRowPlan = updateRowPlan;
		this.deleteRowPlan = deleteRowPlan;
		this.removeOperation = removeOperation;
	}

	public CollectionGraphMutationTarget getTarget() {
		return target;
	}

	public InsertRowPlan getInsertRowPlan() {
		return insertRowPlan;
	}

	public UpdateRowPlan getUpdateRowPlan() {
		return updateRowPlan;
	}

	public DeleteRowPlan getDeleteRowPlan() {
		return deleteRowPlan;
	}

	public MutationOperation getRemoveOperation() {
		return removeOperation;
	}


	@Override
	public String toString() {
		return "CollectionJdbcOperations(" + target.getRolePath() + ")";
	}


	@FunctionalInterface
	public interface Values {
		void applyValues(
				PersistentCollection<?> collection,
				Object key,
				Object rowValue,
				int rowPosition,
				SharedSessionContractImplementor session,
				JdbcValueBindings jdbcValueBindings);
	}

	@FunctionalInterface
	public interface Restrictions {
		void applyRestrictions(
				PersistentCollection<?> collection,
				Object key,
				Object rowValue,
				int rowPosition,
				SharedSessionContractImplementor session,
				JdbcValueBindings jdbcValueBindings);
	}

	public record InsertRowPlan(
			MutationOperation jdbcOperation,
			Values values) {
	}

	public record UpdateRowPlan(
			MutationOperation jdbcOperation,
			Values values,
			Restrictions restrictions) {
	}

	public record DeleteRowPlan(
			MutationOperation jdbcOperation,
			Restrictions restrictions) {
	}
}
