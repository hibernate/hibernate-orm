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
public record CollectionJdbcOperations(
		CollectionMutationTarget target,
		InsertRowPlan insertRowPlan,
		UpdateRowPlan updateRowPlan,
		UpdateRowPlan orderUpdatePlan,
		DeleteRowPlan deleteRowPlan,
		MutationOperation removeOperation) {
	public CollectionJdbcOperations(
			CollectionMutationTarget target,
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
				null, // orderUpdatePlan
				new DeleteRowPlan( deleteRowOperation, deleteRowRestrictions ),
				removeOperation
		);
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
