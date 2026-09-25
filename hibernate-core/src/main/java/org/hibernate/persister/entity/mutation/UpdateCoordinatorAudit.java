package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.audit.ModificationType;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Update coordinator for audited entities.
 */
@Internal
public class UpdateCoordinatorAudit extends AbstractAuditCoordinator implements UpdateCoordinator {
	final UpdateCoordinator currentUpdateCoordinator;

	public UpdateCoordinatorAudit(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull UpdateCoordinator currentUpdateCoordinator) {
		super( entityPersister, factory );
		this.currentUpdateCoordinator = currentUpdateCoordinator;
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentUpdateCoordinator.getStaticMutationOperationGroup();
	}

	@Nullable
	@Override
	public GeneratedValues update(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull Object[] values,
			@Nullable Object oldVersion,
			@Nullable Object[] incomingOldValues,
			@Nullable int[] dirtyAttributeIndexes,
			boolean hasDirtyCollection,
			@Nonnull SharedSessionContractImplementor session) {
		final var generatedValues = currentUpdateCoordinator.update(
				entity,
				id,
				rowId,
				values,
				oldVersion,
				incomingOldValues,
				dirtyAttributeIndexes,
				hasDirtyCollection,
				session
		);
		if ( shouldAuditUpdate( dirtyAttributeIndexes, hasDirtyCollection ) ) {
			enqueueAuditEntry( resolveEntityKey( entity, id, session ), entity, values, ModificationType.MOD, session );
		}
		return generatedValues;
	}

	@Override
	public void forceVersionIncrement(
			@Nonnull Object id,
			@Nullable Object currentVersion,
			@Nonnull Object nextVersion,
			@Nonnull SharedSessionContractImplementor session) {
		currentUpdateCoordinator.forceVersionIncrement( id, currentVersion, nextVersion, session );
	}

	boolean shouldAuditUpdate(@Nullable int[] dirtyAttributeIndexes, boolean hasDirtyCollection) {
		if ( dirtyAttributeIndexes == null || dirtyAttributeIndexes.length == 0 ) {
			return true;
		}
		else if ( hasDirtyCollection ) {
			return true;
		}
		else {
			for ( int dirtyIndex : dirtyAttributeIndexes ) {
				if ( auditedPropertyMask[dirtyIndex] ) {
					return true;
				}
			}
			return false;
		}
	}
}
