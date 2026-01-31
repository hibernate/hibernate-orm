/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

/**
 * DeleteCoordinator for soft-deletes
 *
 * @author Steve Ebersole
 */
public class DeleteCoordinatorSoft extends AbstractDeleteCoordinator {
	public DeleteCoordinatorSoft(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
	}

	@Override
	protected MutationOperationGroup generateOperationGroup(
			Object rowId,
			Object[] loadedState,
			boolean applyVersion,
			SharedSessionContractImplementor session) {
		final var rootTableMapping = entityPersister().getIdentifierTableMapping();
		final var tableUpdateBuilder = new TableUpdateBuilderStandard<>( entityPersister(), rootTableMapping, factory() );

		applyKeyRestriction( rowId, entityPersister(), tableUpdateBuilder, rootTableMapping );
		applySoftDelete( entityPersister().getSoftDeleteMapping(), tableUpdateBuilder );
		applyPartitionKeyRestriction( tableName -> tableUpdateBuilder );
		applyOptimisticLocking(
				entityPersister().optimisticLockStyle(),
				tableMutationBuilderResolver( tableUpdateBuilder ),
				loadedState,
				session
		);

		return createMutationOperationGroup( tableUpdateBuilder );
	}

	private void applySoftDelete(
			SoftDeleteMapping softDeleteMapping,
			TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var softDeleteColumnReference = new ColumnReference( tableUpdateBuilder.getMutatingTable(), softDeleteMapping );

		// apply the assignment
		tableUpdateBuilder.addValueColumn( softDeleteMapping.createDeletedValueBinding( softDeleteColumnReference ) );
		// apply the restriction
		tableUpdateBuilder.addNonKeyRestriction( softDeleteMapping.createNonDeletedValueBinding( softDeleteColumnReference ) );
	}
}
