/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.RootClass;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationPlanContributor;
import org.hibernate.action.queue.internal.decompose.collection.HistoryCollectionMutationPlanContributor;
import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;
import org.hibernate.action.queue.internal.decompose.entity.HistoryEntityMutationPlanContributor;
import org.hibernate.metamodel.mapping.AuxiliaryMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.TemporalMappingImpl;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorHistory;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorHistory;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorHistory;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorHistory;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorNoOp;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.DeleteCoordinatorHistory;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.InsertCoordinatorHistory;
import org.hibernate.persister.entity.mutation.MergeCoordinatorHistory;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinatorHistory;
import org.hibernate.persister.state.spi.StateManagement;
import org.hibernate.persister.state.spi.StateManagementGraphIntegration;
import org.hibernate.persister.state.spi.StateManagementLegacyIntegration;

import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.getTableIdentifierExpression;
import static org.hibernate.persister.state.internal.AbstractStateManagement.isInsertAllowed;
import static org.hibernate.persister.state.internal.AbstractStateManagement.isUpdatePossible;
import static org.hibernate.persister.state.internal.AbstractStateManagement.resolveMutationTarget;

/**
 * State management for temporal entities and collections with
 * {@linkplain org.hibernate.annotations.Temporal.HistoryTable
 * history tables}.
 *
 * @author Gavin King
 *
 * @since 7.4
 */
public final class HistoryStateManagement implements StateManagement, StateManagementLegacyIntegration {
	public static final HistoryStateManagement INSTANCE = new HistoryStateManagement();

	private final StateManagementLegacyIntegration standardLegacyIntegration =
			StandardStateManagement.INSTANCE.getLegacyIntegration();

	private final StateManagementGraphIntegration graphIntegration = new StateManagementGraphIntegration() {
		@Nonnull
		@Override
		public EntityMutationPlanContributor createEntityMutationPlanContributor(@Nonnull EntityPersister persister) {
			return new HistoryEntityMutationPlanContributor( persister, persister.getFactory() );
		}

		@Nonnull
		@Override
		public CollectionMutationPlanContributor createCollectionMutationPlanContributor(@Nonnull CollectionPersister persister) {
			return new HistoryCollectionMutationPlanContributor();
		}
	};

	private HistoryStateManagement() {
	}

	@Nonnull
	@Override
	public StateManagementLegacyIntegration getLegacyIntegration() {
		return this;
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
	public UpdateCoordinator createMergeCoordinator(@Nonnull EntityPersister persister) {
		return new MergeCoordinatorHistory( persister, persister.getFactory(),
				standardLegacyIntegration.createMergeCoordinator( persister ) );
	}

	@Nonnull
	@Override
	public InsertCoordinator createInsertCoordinator(@Nonnull EntityPersister persister) {
		return new InsertCoordinatorHistory( persister, persister.getFactory(),
				standardLegacyIntegration.createInsertCoordinator( persister ) );
	}

	@Nonnull
	@Override
	public UpdateCoordinator createUpdateCoordinator(@Nonnull EntityPersister persister) {
		return new UpdateCoordinatorHistory( persister, persister.getFactory(),
				standardLegacyIntegration.createUpdateCoordinator( persister ) );
	}

	@Nonnull
	@Override
	public DeleteCoordinator createDeleteCoordinator(@Nonnull EntityPersister persister) {
		return new DeleteCoordinatorHistory( persister, persister.getFactory(),
				standardLegacyIntegration.createDeleteCoordinator( persister ) );
	}

	@Nonnull
	@Override
	public InsertRowsCoordinator createInsertRowsCoordinator(@Nonnull CollectionPersister persister) {
		final var mutationTarget = resolveMutationTarget( persister );
		if ( !isInsertAllowed( persister ) ) {
			return new InsertRowsCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return new InsertRowsCoordinatorHistory(
					mutationTarget,
					persister.getRowMutationOperations(),
					standardLegacyIntegration.createInsertRowsCoordinator( persister ),
					persister.getIndexColumnIsSettable(),
					persister.getElementColumnIsSettable(),
					persister.getIndexIncrementer(),
					persister.getFactory().getServiceRegistry()
			);
		}
	}

	@Nonnull
	@Override
	public UpdateRowsCoordinator createUpdateRowsCoordinator(@Nonnull CollectionPersister persister) {
		final var mutationTarget = resolveMutationTarget( persister );
		if ( !isUpdatePossible( persister ) ) {
			return new UpdateRowsCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return new UpdateRowsCoordinatorHistory(
					mutationTarget,
					persister.getRowMutationOperations(),
					persister.getFactory(),
					persister.getIndexColumnIsSettable(),
					persister.getElementColumnIsSettable(),
					persister.getIndexIncrementer()
			);
		}
	}

	@Nonnull
	@Override
	public DeleteRowsCoordinator createDeleteRowsCoordinator(@Nonnull CollectionPersister persister) {
		final var mutationTarget = resolveMutationTarget( persister );
		if ( !persister.needsRemove() ) {
			return new DeleteRowsCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return new DeleteRowsCoordinatorHistory(
					mutationTarget,
					persister.getRowMutationOperations(),
					mutationTarget.hasPhysicalIndexColumn(),
					persister.getIndexColumnIsSettable(),
					persister.getElementColumnIsSettable(),
					persister.getIndexIncrementer(),
					persister.getFactory().getServiceRegistry()
			);
		}
	}

	@Nonnull
	@Override
	public RemoveCoordinator createRemoveCoordinator(@Nonnull CollectionPersister persister) {
		final var mutationTarget = resolveMutationTarget( persister );
		if ( !persister.needsRemove() ) {
			return new RemoveCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return new RemoveCoordinatorHistory(
					mutationTarget,
					persister.getRowMutationOperations(),
					persister.getIndexColumnIsSettable(),
					persister.getElementColumnIsSettable(),
					persister.getIndexIncrementer(),
					persister.getFactory().getServiceRegistry()
			);
		}
	}

	@Nullable
	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			@Nonnull EntityPersister persister,
			@Nonnull RootClass rootClass,
			@Nonnull MappingModelCreationProcess creationProcess) {
		final var temporalTable = rootClass.getAuxiliaryTable();
		String tableName = temporalTable == null
				? persister.getIdentifierTableName()
				: ( (AbstractEntityPersister) persister )
						.determineTableName( temporalTable );
		return new TemporalMappingImpl( rootClass, tableName, creationProcess );
	}

	@Nullable
	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			@Nonnull PluralAttributeMapping pluralAttributeMapping,
			@Nonnull Collection bootDescriptor,
			@Nonnull MappingModelCreationProcess creationProcess) {
		final var temporalTable = bootDescriptor.getAuxiliaryTable();
		String tableName = temporalTable == null
				? pluralAttributeMapping.getSeparateCollectionTable()
				: getTableIdentifierExpression( temporalTable, creationProcess );
		return new TemporalMappingImpl( bootDescriptor, tableName, creationProcess );
	}
}
