/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.decompose.collection;

import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.MutationOperation;

/// Manages standard MutationOperation, as well as parameter binding details, for collection mutations.
/// Built by collection persisters for use in decomposers.
///
/// @param insertRowPlan Plan for inserting a single collection element row
/// @param updateRowPlan Plan for updating a single collection element row
/// @param updateIndexPlan Plan for updating the order/index column for a single collection element row
/// @param deleteRowPlan Plan for deleting a single collection element row
/// @param removeOperation Plan for removing all collection rows
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public record CollectionJdbcOperations(
		CollectionMutationTarget target,
		TableDescriptor tableDescriptor,
		InsertRowPlan insertRowPlan,
		UpdateRowPlan updateRowPlan,
		UpdateRowPlan updateIndexPlan,
		DeleteRowPlan deleteRowPlan,
		MutationOperation removeOperation) {

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
