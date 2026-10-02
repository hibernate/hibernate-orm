package org.hibernate.persister.state.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.AuxiliaryMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;
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
import org.hibernate.persister.state.spi.StateManagementLegacyIntegration;

import static org.hibernate.internal.util.collections.ArrayHelper.isAnyTrue;

/**
 * @author Gavin King
 *
 * @since 7.4
 */
public abstract class AbstractStateManagement implements StateManagement, StateManagementLegacyIntegration {
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Legacy ActionQueue integration

	@Nonnull
	@Override
	public StateManagementLegacyIntegration getLegacyIntegration() {
		return this;
	}

	@Nonnull
	@Override
	public InsertCoordinator createInsertCoordinator(@Nonnull EntityPersister persister) {
		return new InsertCoordinatorStandard( persister, persister.getFactory() );
	}

	@Nonnull
	@Override
	public UpdateCoordinator createUpdateCoordinator(@Nonnull EntityPersister persister) {
		final var attributeMappings = persister.getAttributeMappings();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			if ( attributeMappings.get( i ) instanceof SingularAttributeMapping ) {
				return new UpdateCoordinatorStandard( persister, persister.getFactory() );
			}
		}
		return new UpdateCoordinatorNoOp( persister );
	}

	@Nonnull
	@Override
	public UpdateCoordinator createMergeCoordinator(@Nonnull EntityPersister persister) {
		return new MergeCoordinatorStandard( persister, persister.getFactory() );
	}

	@Nonnull
	@Override
	public DeleteCoordinator createDeleteCoordinator(@Nonnull EntityPersister persister) {
		return new DeleteCoordinatorStandard( persister, persister.getFactory() );
	}

	@Nonnull
	@Override
	public InsertRowsCoordinator createInsertRowsCoordinator(@Nonnull CollectionPersister persister) {
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

	@Nonnull
	@Override
	public UpdateRowsCoordinator createUpdateRowsCoordinator(@Nonnull CollectionPersister persister) {
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
						(OneToManyPersister) mutationTarget,
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

	@Nonnull
	@Override
	public DeleteRowsCoordinator createDeleteRowsCoordinator(@Nonnull CollectionPersister persister) {
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

	@Nonnull
	@Override
	public RemoveCoordinator createRemoveCoordinator(@Nonnull CollectionPersister persister) {
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

	protected static boolean isUpdatePossible(@Nonnull CollectionPersister persister) {
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

	protected static boolean isInsertAllowed(@Nonnull CollectionPersister persister) {
		return !persister.isInverse() && persister.isRowInsertEnabled();
	}

	protected boolean isTablePerSubclass(@Nonnull CollectionPersister persister) {
		final var elementPersister = persister.getElementPersister();
		return elementPersister != null
			&& elementPersister.hasSubclasses()
			&& elementPersister instanceof UnionSubclassEntityPersister;
	}

	@Nonnull
	protected static AbstractCollectionPersister resolveMutationTarget(@Nonnull CollectionPersister persister) {
		if ( persister instanceof AbstractCollectionPersister collectionMutationTarget ) {
			return collectionMutationTarget;
		}
		throw new IllegalArgumentException( "CollectionPersister does not implement CollectionMutationTarget" );
	}


	@Nullable
	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			@Nonnull EntityPersister persister,
			@Nonnull RootClass bootDescriptor,
			@Nonnull MappingModelCreationProcess creationProcess) {
		return null;
	}

	@Nullable
	@Override
	public AuxiliaryMapping createAuxiliaryMapping(
			@Nonnull PluralAttributeMapping pluralAttributeMapping,
			@Nonnull Collection bootDescriptor,
			@Nonnull MappingModelCreationProcess creationProcess) {
		return null;
	}
}
