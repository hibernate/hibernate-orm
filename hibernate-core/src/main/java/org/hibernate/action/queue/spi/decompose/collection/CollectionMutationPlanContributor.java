/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.decompose.collection;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.action.queue.spi.meta.CollectionTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.collection.spi.CollectionChangeSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.model.MutationOperation;

/// Contributes state-management-specific collection mutation plans for graph queue decomposition.
///
/// The collection decomposer is responsible for discovering collection deltas and assigning stable
/// graph ordinals.  This contract is responsible for changing how those logical deltas are modeled
/// for a specific state-management strategy.  For example, single-table temporal state management
/// replaces physical row deletes with temporal end-row updates and models value changes as end plus
/// insert instead of in-place update.
///
/// Contributors may replace the JDBC mutation plan for row delete and remove-all operations and may
/// contribute their own flush operations for value changes.  When a contributor replaces remove-all
/// SQL, it can also bind any strategy-specific assignment values through
/// [#bindRemoveValues(RemoveBindContext, JdbcValueBindings)].
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public interface CollectionMutationPlanContributor {
	/// The contributor used by the standard state-management model.  It does
	/// not contribute any alternate collection mutation plans.
	CollectionMutationPlanContributor STANDARD = new CollectionMutationPlanContributor() {
	};

	/// Optionally replaces the mutation plan for deleting a single collection row.
	///
	/// The supplied table descriptor is the physical table being mutated by the
	/// decomposer.  For table-per-subclass one-to-many collections this may be a
	/// concrete entity table descriptor rather than the collection table descriptor.
	///
	/// @param context Details needed to build the row-delete mutation plan.
	/// @param standardPlanSupplier Supplies the decomposer's normal row-delete plan.
	///
	/// @return The row-delete plan to use for this collection table shape.
	default CollectionJdbcOperations.DeleteRowPlan buildDeleteRowPlan(
			DeleteRowPlanContext context,
			Supplier<CollectionJdbcOperations.DeleteRowPlan> standardPlanSupplier) {
		return standardPlanSupplier.get();
	}

	/// Optionally replaces the mutation operation for removing all rows for a collection key.
	///
	/// The returned operation is used by the decomposer's remove-all
	/// [FlushOperation].  Contributors which add assignment parameters to the
	/// replacement operation should also override
	/// [#bindRemoveValues(RemoveBindContext, JdbcValueBindings)].
	///
	/// @param context Details needed to build the remove-all mutation operation.
	/// @param standardOperationSupplier Supplies the decomposer's normal remove-all operation.
	///
	/// @return The remove-all mutation operation to use, or `null` if no SQL operation is needed.
	default MutationOperation buildRemoveOperation(
			RemoveOperationContext context,
			Supplier<MutationOperation> standardOperationSupplier) {
		return standardOperationSupplier.get();
	}

	/// Optionally replaces the flush operations for an in-place collection value change.
	///
	/// A contributor should return `true` only when it has emitted the complete
	/// operation set for the value change.  Returning `false` lets the decomposer
	/// emit its standard update-row operation.
	///
	/// @param context Details of the logical value change.
	/// @param operationConsumer Consumer for contributed flush operations.
	///
	/// @return `true` if the contributor fully handled the value change; `false` otherwise.
	default boolean contributeValueChange(
			ValueChangeContext context,
			Consumer<FlushOperation> operationConsumer) {
		return false;
	}

	/// Optionally contributes extra operations for a single logical row insertion.
	///
	/// This hook is row-scoped.  It is suitable for strategies that need to
	/// mirror or augment each inserted row, such as inserting a history-table row.
	/// Contributors that only need to observe the whole collection action should
	/// usually use [#contributeCollectionChange(CollectionChangeContext, Consumer)].
	///
	/// @param context Details of the inserted logical collection row.
	/// @param operationConsumer Consumer for contributed flush operations.
	default void contributeAdditionalInsert(
			RowInsertContext context,
			Consumer<FlushOperation> operationConsumer) {
	}

	/// Optionally contributes operations or callbacks for a logical collection change.
	///
	/// This hook is action-scoped rather than row-scoped.  It is called when a
	/// decomposer has detected work for the collection action and is intended for
	/// contributors that need to record or defer whole-collection state changes,
	/// such as audit collection-change collection.
	///
	/// @param context Details of the logical collection change.
	/// @param operationConsumer Consumer for contributed flush operations.
	default void contributeCollectionChange(
			CollectionChangeContext context,
			Consumer<FlushOperation> operationConsumer) {
	}

	/// Binds parameter values for a remove-all collection operation.
	///
	/// The default implementation binds the collection key as a restriction.  A
	/// contributor that replaces remove-all SQL with an update, such as
	/// single-table temporal state management, may bind assignment values before
	/// or after delegating to this default implementation.
	///
	/// @param context Details of the remove-all binding.
	/// @param valueBindings Binding target for JDBC values.
	default void bindRemoveValues(
			RemoveBindContext context,
			JdbcValueBindings valueBindings) {
		context.persister().getAttributeMapping().getKeyDescriptor().getKeyPart().decompose(
				context.key(),
				(valueIndex, value, jdbcValueMapping) -> valueBindings.bindValue(
						value,
						jdbcValueMapping.getSelectionExpression(),
						ParameterUsage.RESTRICT
				),
				context.session()
		);
	}

	/// Context passed to [#buildDeleteRowPlan(DeleteRowPlanContext, Supplier)].
	///
	/// @param persister The collection persister whose logical row is being deleted.
	/// @param tableDescriptor The physical table descriptor used by the decomposer.
	/// @param sqlWhereString Any SQL restriction fragment associated with the collection table.
	/// @param factory The session factory.
	record DeleteRowPlanContext(
			CollectionPersister persister,
			TableDescriptor tableDescriptor,
			String sqlWhereString,
			SessionFactoryImplementor factory) {
	}

	/// Context passed to [#buildRemoveOperation(RemoveOperationContext, Supplier)].
	///
	/// @param persister The collection persister whose rows are being removed.
	/// @param tableDescriptor The physical table descriptor used by the decomposer.
	/// @param sqlWhereString Any SQL restriction fragment associated with the collection table.
	/// @param factory The session factory.
	record RemoveOperationContext(
			CollectionPersister persister,
			TableDescriptor tableDescriptor,
			String sqlWhereString,
			SessionFactoryImplementor factory) {
	}

	/// Context passed to [#bindRemoveValues(RemoveBindContext, JdbcValueBindings)].
	///
	/// @param persister The collection persister whose rows are being removed.
	/// @param key The collection key identifying rows to remove.
	/// @param session The active session.
	record RemoveBindContext(
			CollectionPersister persister,
			Object key,
			SharedSessionContractImplementor session) {
	}

	/// Context passed to [#contributeAdditionalInsert(RowInsertContext, Consumer)].
	///
	/// @param persister The collection persister whose row is being inserted.
	/// @param tableDescriptor The physical table descriptor used by the row insert plan.
	/// @param factory The session factory.
	/// @param jdbcOperations The decomposer's JDBC operation bundle for this table shape.
	/// @param collection The persistent collection instance.
	/// @param key The collection key.
	/// @param rowValue The row value passed to the row insert bind plan.
	/// @param rowPosition The row position passed to the row insert bind plan.
	/// @param ordinalBase The graph ordinal base assigned to the enclosing action.
	record RowInsertContext(
			CollectionPersister persister,
			TableDescriptor tableDescriptor,
			SessionFactoryImplementor factory,
			CollectionJdbcOperations jdbcOperations,
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			int ordinalBase) {
	}

	/// Context passed to [#contributeCollectionChange(CollectionChangeContext, Consumer)].
	///
	/// @param persister The collection persister whose logical collection changed.
	/// @param tableDescriptor The collection table descriptor for the logical collection role.
	/// @param factory The session factory.
	/// @param jdbcOperations The decomposer's JDBC operation bundle, when a single bundle applies.
	/// @param collection The persistent collection instance.
	/// @param key The collection key.
	/// @param ordinalBase The graph ordinal base assigned to the enclosing action.
	record CollectionChangeContext(
			CollectionPersister persister,
			CollectionTableDescriptor tableDescriptor,
			SessionFactoryImplementor factory,
			CollectionJdbcOperations jdbcOperations,
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase) {
	}

	/// Context passed to [#contributeValueChange(ValueChangeContext, Consumer)].
	///
	/// @param persister The collection persister whose row value changed.
	/// @param tableDescriptor The physical table descriptor used by the row update plan.
	/// @param jdbcOperations The decomposer's JDBC operation bundle for this table shape.
	/// @param collection The persistent collection instance.
	/// @param key The collection key.
	/// @param ordinalBase The graph ordinal base assigned to the enclosing action.
	/// @param session The active session.
	/// @param valueChange The logical value change.
	record ValueChangeContext(
			CollectionPersister persister,
			TableDescriptor tableDescriptor,
			CollectionJdbcOperations jdbcOperations,
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			CollectionChangeSet.ValueChange valueChange) {
	}
}
