package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.noOperations;

/**
 * @author Steve Ebersole
 */
@org.hibernate.Internal
public class UpdateCoordinatorNoOp implements UpdateCoordinator {
	private final MutationOperationGroup operationGroup;

	public UpdateCoordinatorNoOp(@Nonnull EntityPersister entityPersister) {
		operationGroup = noOperations( MutationType.UPDATE, entityPersister );
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return operationGroup;
	}

	@Nullable
	@Override
	public GeneratedValues update(@Nonnull Object entity, @Nonnull Object id, @Nullable Object rowId, @Nonnull Object[] values, @Nullable Object oldVersion, @Nullable Object[] incomingOldValues, @Nullable int[] dirtyAttributeIndexes, boolean hasDirtyCollection, @Nonnull SharedSessionContractImplementor session) {
		// nothing to do
		return null;
	}

	@Override
	public void forceVersionIncrement(@Nonnull Object id, @Nullable Object currentVersion, @Nonnull Object nextVersion, @Nonnull SharedSessionContractImplementor session) {
		// nothing to do
	}
}
