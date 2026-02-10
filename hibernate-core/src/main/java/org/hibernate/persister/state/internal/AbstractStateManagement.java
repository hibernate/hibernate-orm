/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.AuxiliaryMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.collection.mutation.CollectionMutationTarget;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorStandard;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorTablePerSubclass;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorStandard;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorTablePerSubclass;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorStandard;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorTablePerSubclass;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorOneToMany;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorStandard;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorTablePerSubclass;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.DeleteCoordinatorStandard;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.InsertCoordinatorStandard;
import org.hibernate.persister.entity.mutation.MergeCoordinatorStandard;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinatorNoOp;
import org.hibernate.persister.entity.mutation.UpdateCoordinatorStandard;
import org.hibernate.persister.state.spi.StateManagement;

import static org.hibernate.internal.util.collections.ArrayHelper.isAnyTrue;

/**
 * @author Gavin King
 *
 * @since 7.4
 */
abstract class AbstractStateManagement implements StateManagement {
	@Override
	public InsertCoordinator createInsertCoordinator(EntityPersister persister) {
		return new InsertCoordinatorStandard( persister, persister.getFactory() );
	}

	@Override
	public UpdateCoordinator createUpdateCoordinator(EntityPersister persister) {
		final var attributeMappings = persister.getAttributeMappings();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			if ( attributeMappings.get( i ) instanceof SingularAttributeMapping ) {
				return new UpdateCoordinatorStandard( persister, persister.getFactory() );
			}
		}
		return new UpdateCoordinatorNoOp( persister );
	}

	@Override
	public UpdateCoordinator createMergeCoordinator(EntityPersister persister) {
		return new MergeCoordinatorStandard( persister, persister.getFactory() );
	}

	@Override
	public DeleteCoordinator createDeleteCoordinator(EntityPersister persister) {
		return new DeleteCoordinatorStandard( persister, persister.getFactory() );
	}

	@Override
	public InsertRowsCoordinator createInsertRowsCoordinator(CollectionPersister persister) {
		final var mutationTarget = resolveMutationTarget( persister );
		if ( !isInsertAllowed( persister ) ) {
			return new InsertRowsCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() && isTablePerSubclass( persister ) ) {
			return new InsertRowsCoordinatorTablePerSubclass(
					(OneToManyPersister) mutationTarget,
					persister.getRowMutationOperations(),
					persister.getFactory().getServiceRegistry()
			);
		}
		else {
			return new InsertRowsCoordinatorStandard(
					mutationTarget,
					persister.getRowMutationOperations(),
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
			if ( isTablePerSubclass( persister ) ) {
				return new UpdateRowsCoordinatorTablePerSubclass(
						(OneToManyPersister) mutationTarget,
						persister.getRowMutationOperations(),
						persister.getFactory()
				);
			}
			else {
				return new UpdateRowsCoordinatorOneToMany(
						mutationTarget,
						persister.getRowMutationOperations(),
						persister.getFactory()
				);
			}
		}
		else {
			return new UpdateRowsCoordinatorStandard(
					mutationTarget,
					persister.getRowMutationOperations(),
					persister.getFactory()
			);
		}
	}

	@Override
	public DeleteRowsCoordinator createDeleteRowsCoordinator(CollectionPersister persister) {
		final var mutationTarget = resolveMutationTarget( persister );
		if ( !persister.needsRemove() ) {
			return new DeleteRowsCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() && isTablePerSubclass( persister ) ) {
			return new DeleteRowsCoordinatorTablePerSubclass(
					(OneToManyPersister) mutationTarget,
					persister.getRowMutationOperations(),
					false,
					persister.getFactory().getServiceRegistry()
			);
		}
		else {
			return new DeleteRowsCoordinatorStandard(
					mutationTarget,
					persister.getRowMutationOperations(),
					!persister.isOneToMany()
							&& mutationTarget.hasPhysicalIndexColumn(),
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
		else if ( persister.isOneToMany() && isTablePerSubclass( persister ) ) {
			return new RemoveCoordinatorTablePerSubclass(
					(OneToManyPersister) mutationTarget,
					persister.getRowMutationOperations(),
					persister.getFactory().getServiceRegistry()
			);
		}
		else {
			return new RemoveCoordinatorStandard(
					mutationTarget,
					persister.getRowMutationOperations(),
					persister.getFactory().getServiceRegistry()
			);
		}
	}

	protected static boolean isUpdatePossible(CollectionPersister persister) {
		if ( persister.isOneToMany() ) {
			return persister.isRowDeleteEnabled()
				|| persister.isRowInsertEnabled();
		}
		else {
			return !persister.isInverse()
				&& persister.getCollectionSemantics().getCollectionClassification().isRowUpdatePossible()
				&& isAnyTrue( persister.getElementColumnIsSettable() );
		}
	}

	protected static boolean isInsertAllowed(CollectionPersister persister) {
		return !persister.isInverse() && persister.isRowInsertEnabled();
	}

	protected boolean isTablePerSubclass(CollectionPersister persister) {
		final var elementPersister = persister.getElementPersister();
		return elementPersister != null
			&& elementPersister.hasSubclasses()
			&& elementPersister instanceof UnionSubclassEntityPersister;
	}

	protected static CollectionMutationTarget resolveMutationTarget(CollectionPersister persister) {
		if ( persister instanceof CollectionMutationTarget collectionMutationTarget ) {
			return collectionMutationTarget;
		}
		throw new IllegalArgumentException( "CollectionPersister does not implement CollectionMutationTarget" );
	}


	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			EntityPersister persister,
			RootClass rootClass,
			MappingModelCreationProcess creationProcess) {
		return null;
	}

	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			PluralAttributeMapping pluralAttributeMapping,
			Collection bootDescriptor,
			MappingModelCreationProcess creationProcess) {
		return null;
	}
}
