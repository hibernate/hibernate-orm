/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilder;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilderSkipped;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilderStandard;

/**
 * Coordinates standard deleting of an entity.
 *
 * @author Steve Ebersole
 */
public class DeleteCoordinatorStandard extends AbstractDeleteCoordinator {

	public DeleteCoordinatorStandard(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
	}

	@Override
	protected MutationOperationGroup generateOperationGroup(
			Object rowId,
			Object[] loadedState,
			boolean applyVersion,
			SharedSessionContractImplementor session) {
		final var deleteGroupBuilder = new MutationGroupBuilder( MutationType.DELETE, entityPersister() );

		entityPersister().forEachMutableTableReverse( tableMapping ->
				deleteGroupBuilder.addTableDetailsBuilder( tableMapping.isCascadeDeleteEnabled()
						? new TableDeleteBuilderSkipped( tableMapping )
						: new TableDeleteBuilderStandard( entityPersister(), tableMapping, factory() ) ) );

		applyTableDeleteDetails( deleteGroupBuilder, rowId, loadedState, applyVersion, session );

		return createOperationGroup( null, deleteGroupBuilder.buildMutationGroup() );
	}

	private void applyTableDeleteDetails(
			MutationGroupBuilder deleteGroupBuilder,
			Object rowId,
			Object[] loadedState,
			boolean applyVersion,
			SharedSessionContractImplementor session) {
		// first, the table key column(s)
		deleteGroupBuilder.forEachTableMutationBuilder( builder ->
				applyKeyRestriction( rowId, entityPersister(), (TableDeleteBuilder) builder,
						(EntityTableMapping) builder.getMutatingTable().getTableMapping() ) );

		if ( applyVersion ) {
			// apply any optimistic locking
			applyOptimisticLocking(
					entityPersister().optimisticLockStyle(),
					deleteGroupBuilder::findTableDetailsBuilder,
					loadedState,
					session
			);
			applyPartitionKeyRestriction( deleteGroupBuilder::findTableDetailsBuilder );
		}
	}

}
