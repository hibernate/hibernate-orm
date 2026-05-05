/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.ast.TableUpdate;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

import java.util.function.Consumer;
import java.util.Map;

/// [Decomposer][EntityActionDecomposer] for entity delete operations for entities mapped with soft-delete.
///
/// @author Steve Ebersole
public class DeleteDecomposerSoftDelete extends AbstractDeleteDecomposer {
	private final TableUpdate<?> softDeleteOperation;

	public DeleteDecomposerSoftDelete(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		super( entityPersister, sessionFactory );
		assert entityPersister.getSoftDeleteMapping() != null;

		this.softDeleteOperation = generateSoftDeleteOperation();
	}

	@Override
	public Map<String, ? extends TableMutation<?>> getStaticDeleteOperations() {
		return Map.of( softDeleteOperation.getTableName(), softDeleteOperation );
	}

	@Override
	public void decompose(
			EntityDeleteAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<PlannedOperation> operationConsumer) {
		final Object naturalIdValues = DeleteNaturalIdHandling.removeLocalResolution( action, session );

		final DeleteCacheHandling.CacheLock cacheLock = DeleteCacheHandling.lockItem( action, session );
		registerAfterTransactionCompletion( action, cacheLock, session );

		final PreDeleteHandling preDeleteHandling = new PreDeleteHandling( action );
		final PostDeleteHandling postDeleteHandling = new PostDeleteHandling(
				action,
				cacheLock.cacheKey(),
				naturalIdValues,
				preDeleteHandling
		);

		final Object identifier = action.getId();
		final Object version = action.getVersion();

		final var mutation = softDeleteOperation.createMutationOperation(null, sessionFactory);
		final var tableMapping = (TableDescriptorAsTableMapping) mutation.getTableDetails();
		final var tableDescriptor = (EntityTableDescriptor) tableMapping.getDescriptor();

		final EntitySoftDeleteBindPlan bindPlan = new EntitySoftDeleteBindPlan(
				tableDescriptor,
				entityPersister,
				identifier,
				version,
				// todo : do we need to get loadedState here?
				null,
				OptimisticLockStyle.NONE
		);

		final PlannedOperation op = new PlannedOperation(
				tableDescriptor,
				MutationKind.UPDATE,
				mutation,
				bindPlan,
				ordinalBase * 1_000,
				"EntityDeleteAction(" + entityPersister.getEntityName() + ")"
		);
		op.setPreExecutionCallback( preDeleteHandling );

		// Attach post-execution callback to the operation
		op.setPostExecutionCallback( postDeleteHandling );

		operationConsumer.accept( op );
	}

	private void registerAfterTransactionCompletion(
			EntityDeleteAction action,
			DeleteCacheHandling.CacheLock cacheLock,
			SharedSessionContractImplementor session) {
		final var callback = new DeleteAfterTransactionCompletionHandling( action, cacheLock );
		if ( callback.isNeeded( session ) ) {
			session.getTransactionCompletionCallbacks().registerCallback( callback );
		}
	}

	private TableUpdate<?> generateSoftDeleteOperation() {
		final var softDeleteMapping = entityPersister.getSoftDeleteMapping();
		assert softDeleteMapping != null;

		// Soft delete only operates on the root/identifier table
		final var rootTableDescriptor = entityPersister.getIdentifierTableDescriptor();

		// Create adapter to convert TableDescriptor to TableMapping
		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				rootTableDescriptor,
				0, // relativePosition
				true, // isIdentifierTable
				false // isInverse
		);

		final TableUpdateBuilder<?> tableUpdateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference(tableMapping),
				sessionFactory
		);

		// Apply key restriction (WHERE id = ?)
		tableUpdateBuilder.addKeyRestrictions( rootTableDescriptor.keyDescriptor() );

		// Apply soft delete assignment and restriction
		applySoftDelete( softDeleteMapping, tableUpdateBuilder );

		// Apply partition key restriction if needed
		applyPartitionKeyRestrictionForSoftDelete( tableUpdateBuilder );

		// Version-based optimistic locking (if applicable)
		if ( entityPersister.optimisticLockStyle().isVersion() && entityPersister.getVersionMapping() != null ) {
			tableUpdateBuilder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}

		return (TableUpdate<?>) tableUpdateBuilder.buildMutation();
	}

	private void applySoftDelete(
			SoftDeleteMapping softDeleteMapping,
			TableUpdateBuilder<?> tableUpdateBuilder) {
		final var softDeleteColumnReference = new ColumnReference(
				tableUpdateBuilder.getMutatingTable(),
				softDeleteMapping
		);

		// Apply the assignment: SET deleted_column = deleted_value
		tableUpdateBuilder.addColumnAssignment( softDeleteMapping.createDeletedValueBinding( softDeleteColumnReference ) );

		// Apply the restriction: WHERE deleted_column = not_deleted_value
		tableUpdateBuilder.addNonKeyRestriction( softDeleteMapping.createNonDeletedValueBinding( softDeleteColumnReference ) );
	}

	private void applyPartitionKeyRestrictionForSoftDelete(TableUpdateBuilder<?> tableUpdateBuilder) {
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			final var attributeMappings = entityPersister.getAttributeMappings();
			for ( int m = 0; m < attributeMappings.size(); m++ ) {
				final var attributeMapping = attributeMappings.get( m );
				final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
				for ( int i = 0; i < jdbcTypeCount; i++ ) {
					final var selectableMapping = attributeMapping.getSelectable( i );
					if ( selectableMapping.isPartitioned() ) {
						tableUpdateBuilder.addKeyRestriction( selectableMapping );
					}
				}
			}
		}
	}
}
