/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.action.queue.meta.CollectionTableDescriptor;
import org.hibernate.collection.spi.CollectionChangeSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.sql.model.MutationOperation;

/**
 * Contributes state-management-specific collection mutation plans for graph queue decomposition.
 * <p>
 * The collection decomposer is responsible for discovering collection deltas and assigning stable
 * graph ordinals.  This contract is responsible for changing how those logical deltas are modeled
 * for a specific state-management strategy.  For example, single-table temporal state management
 * replaces physical row deletes with temporal end-row updates and models value changes as end plus
 * insert instead of in-place update.
 * <p>
 * Contributors may replace the JDBC mutation plan for row delete and remove-all operations and may
 * contribute their own flush operations for value changes.  When a contributor replaces remove-all
 * SQL, it can also bind any strategy-specific assignment values through
 * {@link #bindRemoveValues(RemoveBindContext, JdbcValueBindings)}.
 *
 * @author Steve Ebersole
 */
public interface CollectionMutationPlanContributor {
	CollectionMutationPlanContributor STANDARD = new CollectionMutationPlanContributor() {
	};

	default CollectionJdbcOperations.DeleteRowPlan buildDeleteRowPlan(
			DeleteRowPlanContext context,
			Supplier<CollectionJdbcOperations.DeleteRowPlan> standardPlanSupplier) {
		return standardPlanSupplier.get();
	}

	default MutationOperation buildRemoveOperation(
			RemoveOperationContext context,
			Supplier<MutationOperation> standardOperationSupplier) {
		return standardOperationSupplier.get();
	}

	default boolean contributeValueChange(
			ValueChangeContext context,
			Consumer<FlushOperation> operationConsumer) {
		return false;
	}

	default void contributeAdditionalInsert(
			RowInsertContext context,
			Consumer<FlushOperation> operationConsumer) {
	}

	default void bindRemoveValues(
			RemoveBindContext context,
			JdbcValueBindings valueBindings) {
		CollectionMutationPlanSupport.bindRemoveRestrictions(
				context.persister(),
				context.key(),
				context.session(),
				valueBindings
		);
	}

	record DeleteRowPlanContext(
			BasicCollectionPersister persister,
			CollectionTableDescriptor tableDescriptor,
			SessionFactoryImplementor factory) {
	}

	record RemoveOperationContext(
			BasicCollectionPersister persister,
			CollectionTableDescriptor tableDescriptor,
			SessionFactoryImplementor factory) {
	}

	record RemoveBindContext(
			BasicCollectionPersister persister,
			Object key,
			SharedSessionContractImplementor session) {
	}

	record RowInsertContext(
			BasicCollectionPersister persister,
			CollectionTableDescriptor tableDescriptor,
			SessionFactoryImplementor factory,
			CollectionJdbcOperations jdbcOperations,
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			int ordinalBase) {
	}

	record ValueChangeContext(
			BasicCollectionPersister persister,
			CollectionTableDescriptor tableDescriptor,
			CollectionJdbcOperations jdbcOperations,
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			CollectionChangeSet.ValueChange valueChange) {
	}
}
