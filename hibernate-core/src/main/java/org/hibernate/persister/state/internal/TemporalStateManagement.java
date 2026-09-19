/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.TemporalMappingImpl;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorTemporal;
import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationPlanContributor;
import org.hibernate.action.queue.internal.decompose.collection.TemporalCollectionMutationPlanContributor;
import org.hibernate.action.queue.internal.decompose.entity.TemporalEntityMutationPlanContributor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.DeleteCoordinatorTemporal;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.InsertCoordinatorTemporal;
import org.hibernate.persister.entity.mutation.MergeCoordinatorTemporal;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinatorTemporal;
import org.hibernate.persister.state.spi.StateManagementGraphIntegration;
import org.hibernate.temporal.TemporalTableStrategy;

import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.getTableIdentifierExpression;

/**
 * State management for temporal entities and collections in the
 * {@linkplain TemporalTableStrategy#SINGLE_TABLE
 * single table strategy}.
 *
 * @author Gavin King
 *
 * @since 7.4
 */
public final class TemporalStateManagement extends AbstractStateManagement {
	public static final TemporalStateManagement INSTANCE = new TemporalStateManagement();

	private final StateManagementGraphIntegration graphIntegration = new StateManagementGraphIntegration() {
		@Nonnull
		@Override
		public EntityMutationPlanContributor createEntityMutationPlanContributor(@Nonnull EntityPersister persister) {
			return new TemporalEntityMutationPlanContributor( persister, persister.getFactory() );
		}

		@Nonnull
		@Override
		public CollectionMutationPlanContributor createCollectionMutationPlanContributor(@Nonnull CollectionPersister persister) {
			return new TemporalCollectionMutationPlanContributor();
		}
	};

	private TemporalStateManagement() {
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Graph ActionQueue integration

	@Nonnull
	@Override
	public StateManagementGraphIntegration getGraphIntegration() {
		return graphIntegration;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Legacy ActionQueue integration

	@Nonnull
	@Override
	public InsertCoordinator createInsertCoordinator(@Nonnull EntityPersister persister) {
		return new InsertCoordinatorTemporal( persister, persister.getFactory() );
	}

	@Nonnull
	@Override
	public UpdateCoordinator createUpdateCoordinator(@Nonnull EntityPersister persister) {
		return new UpdateCoordinatorTemporal( persister, persister.getFactory() );
	}

	@Nonnull
	@Override
	public UpdateCoordinator createMergeCoordinator(@Nonnull EntityPersister persister) {
		return new MergeCoordinatorTemporal( persister, persister.getFactory() );
	}

	@Nonnull
	@Override
	public DeleteCoordinator createDeleteCoordinator(@Nonnull EntityPersister persister) {
		return new DeleteCoordinatorTemporal( persister, persister.getFactory() );
	}

	@Nonnull
	@Override
	public UpdateRowsCoordinator createUpdateRowsCoordinator(@Nonnull CollectionPersister persister) {
		if ( !isUpdatePossible( persister ) ) {
			return new UpdateRowsCoordinatorNoOp( resolveMutationTarget( persister ) );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return new UpdateRowsCoordinatorTemporal(
					resolveMutationTarget( persister ),
					persister.getRowMutationOperations(),
					persister.getFactory()
			);
		}
	}

	@Nullable
	@Override
	public TemporalMapping createAuxiliaryMapping(
			@Nonnull EntityPersister persister,
			@Nonnull RootClass rootClass,
			@Nonnull MappingModelCreationProcess creationProcess) {
		final var temporalTable = rootClass.getAuxiliaryTable();
		final String tableName = temporalTable == null
				? persister.getIdentifierTableName()
				: ( (AbstractEntityPersister) persister )
						.determineTableName( temporalTable );
		return new TemporalMappingImpl( rootClass, tableName, creationProcess );
	}

	@Nullable
	@Override
	public TemporalMapping createAuxiliaryMapping(
			@Nonnull PluralAttributeMapping pluralAttributeMapping,
			@Nonnull Collection bootDescriptor,
			@Nonnull MappingModelCreationProcess creationProcess) {
		final var temporalTable = bootDescriptor.getAuxiliaryTable();
		final String tableName = temporalTable == null
				? pluralAttributeMapping.getSeparateCollectionTable()
				: getTableIdentifierExpression( temporalTable, creationProcess );
		return new TemporalMappingImpl( bootDescriptor, tableName, creationProcess );
	}
}
