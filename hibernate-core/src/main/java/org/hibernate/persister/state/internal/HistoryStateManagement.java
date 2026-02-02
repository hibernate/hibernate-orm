/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import org.hibernate.Internal;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.RootClass;
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
import org.hibernate.persister.state.StateManagement;

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
 */
@Internal
public final class HistoryStateManagement implements StateManagement {
	public static final HistoryStateManagement INSTANCE = new HistoryStateManagement();

	private HistoryStateManagement() {
	}

	@Override
	public UpdateCoordinator createMergeCoordinator(EntityPersister persister) {
		return new MergeCoordinatorHistory( persister, persister.getFactory(),
				StandardStateManagement.INSTANCE.createMergeCoordinator( persister ) );
	}

	@Override
	public InsertCoordinator createInsertCoordinator(EntityPersister persister) {
		return new InsertCoordinatorHistory( persister, persister.getFactory(),
				StandardStateManagement.INSTANCE.createInsertCoordinator( persister ) );
	}

	@Override
	public UpdateCoordinator createUpdateCoordinator(EntityPersister persister) {
		return new UpdateCoordinatorHistory( persister, persister.getFactory(),
				StandardStateManagement.INSTANCE.createUpdateCoordinator( persister ) );
	}

	@Override
	public DeleteCoordinator createDeleteCoordinator(EntityPersister persister) {
		return new DeleteCoordinatorHistory( persister, persister.getFactory(),
				StandardStateManagement.INSTANCE.createDeleteCoordinator( persister ) );
	}

	@Override
	public InsertRowsCoordinator createInsertRowsCoordinator(CollectionPersister persister) {
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
					StandardStateManagement.INSTANCE.createInsertRowsCoordinator( persister ),
					persister.getIndexColumnIsSettable(),
					persister.getElementColumnIsSettable(),
					persister.getIndexIncrementer(),
					persister.getFactory().getServiceRegistry()
			);
		}
	}

	@Override
	public UpdateRowsCoordinator createUpdateRowsCoordinator(CollectionPersister persister) {
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

	@Override
	public DeleteRowsCoordinator createDeleteRowsCoordinator(CollectionPersister persister) {
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

	@Override
	public RemoveCoordinator createRemoveCoordinator(CollectionPersister persister) {
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

	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			EntityPersister persister,
			RootClass rootClass,
			MappingModelCreationProcess creationProcess) {
		final var temporalTable = rootClass.getAuxiliaryTable();
		String tableName = temporalTable == null
				? persister.getIdentifierTableName()
				: ( (AbstractEntityPersister) persister )
						.determineTableName( temporalTable );
		return new TemporalMappingImpl( rootClass, tableName, creationProcess );
	}

	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			PluralAttributeMapping pluralAttributeMapping,
			Collection bootDescriptor,
			MappingModelCreationProcess creationProcess) {
		final var temporalTable = bootDescriptor.getAuxiliaryTable();
		String tableName = temporalTable == null
				? pluralAttributeMapping.getSeparateCollectionTable()
				: getTableIdentifierExpression( temporalTable, creationProcess );
		return new TemporalMappingImpl( bootDescriptor, tableName, creationProcess );
	}
}
