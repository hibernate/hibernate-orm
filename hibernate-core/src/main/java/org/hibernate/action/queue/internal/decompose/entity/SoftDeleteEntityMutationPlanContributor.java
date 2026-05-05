/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;

import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.ast.TableUpdate;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

/// Graph mutation plan contributor for soft-delete entity deletes.
///
/// @author Steve Ebersole
public class SoftDeleteEntityMutationPlanContributor implements EntityMutationPlanContributor {
	private final EntityPersister entityPersister;
	private final SessionFactoryImplementor sessionFactory;
	private final TableUpdate<?> softDeleteOperation;

	public SoftDeleteEntityMutationPlanContributor(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		this.entityPersister = entityPersister instanceof UnionSubclassEntityPersister
				? entityPersister
				: entityPersister.getRootEntityDescriptor().getEntityPersister();
		this.sessionFactory = sessionFactory;
		this.softDeleteOperation = generateSoftDeleteOperation();
	}

	@Override
	public Map<String, ? extends TableMutation<?>> getStaticDeleteOperations() {
		return Map.of( softDeleteOperation.getTableName(), softDeleteOperation );
	}

	@Override
	public boolean contributeReplacementDelete(
			DeleteContext context,
			Consumer<FlushOperation> operationConsumer) {
		final Object identifier = context.identifier();
		final Object version = context.version();

		final var mutation = softDeleteOperation.createMutationOperation( null, sessionFactory );
		final var tableMapping = (TableDescriptorAsTableMapping) mutation.getTableDetails();
		final var tableDescriptor = (EntityTableDescriptor) tableMapping.descriptor();

		final EntitySoftDeleteBindPlan bindPlan = new EntitySoftDeleteBindPlan(
				tableDescriptor,
				entityPersister,
				identifier,
				version,
				null,
				OptimisticLockStyle.NONE
		);

		final FlushOperation operation = new FlushOperation(
				tableDescriptor,
				MutationKind.UPDATE,
				mutation,
				bindPlan,
				context.ordinalBase() * 1_000,
				"EntityDeleteAction(" + entityPersister.getEntityName() + ")"
		);
		operation.setPreExecutionCallback( context.postDeleteHandling().getPreDeleteHandling() );
		operation.setPostExecutionCallback( context.postDeleteHandling() );
		operationConsumer.accept( operation );
		return true;
	}

	private TableUpdate<?> generateSoftDeleteOperation() {
		final SoftDeleteMapping softDeleteMapping = entityPersister.getSoftDeleteMapping();
		assert softDeleteMapping != null;

		final EntityTableDescriptor rootTableDescriptor = entityPersister.getIdentifierTableDescriptor();
		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				rootTableDescriptor,
				0,
				true,
				false
		);

		final TableUpdateBuilder<?> tableUpdateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference( tableMapping ),
				sessionFactory
		);

		tableUpdateBuilder.addKeyRestrictions( rootTableDescriptor.keyDescriptor() );
		applySoftDelete( softDeleteMapping, tableUpdateBuilder );
		applyPartitionKeyRestrictionForSoftDelete( tableUpdateBuilder );

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

		tableUpdateBuilder.addColumnAssignment( softDeleteMapping.createDeletedValueBinding( softDeleteColumnReference ) );
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
