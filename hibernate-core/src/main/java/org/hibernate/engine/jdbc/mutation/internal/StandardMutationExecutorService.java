/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;

/**
 * Standard MutationExecutorService implementation
 *
 * @see MutationExecutorServiceInitiator
 *
 * @author Steve Ebersole
 */
public class StandardMutationExecutorService implements MutationExecutorService {
	private final int globalBatchSize;

	public StandardMutationExecutorService(Map<String, Object> configurationValues) {
		this( ConfigurationHelper.getInt( Environment.STATEMENT_BATCH_SIZE, configurationValues, 1 ) );
	}

	public StandardMutationExecutorService(int globalBatchSize) {
		this.globalBatchSize = globalBatchSize;
	}

	@Override
	public MutationExecutor createExecutor(
			BatchKeyAccess batchKeySupplier,
			MutationOperationGroup operationGroup,
			SharedSessionContractImplementor session) {
		// decide whether to use batching - any number > one means to batch
		final Integer sessionBatchSize = session.getJdbcCoordinator()
				.getJdbcSessionOwner()
				.getJdbcBatchSize();
		final int batchSizeToUse = sessionBatchSize == null
				? globalBatchSize
				: sessionBatchSize;

		final int numberOfOperations = operationGroup.getNumberOfOperations();
		final MutationType mutationType = operationGroup.getMutationType();
		final MutationTarget<?> mutationTarget = operationGroup.getMutationTarget();

		if ( mutationType == MutationType.INSERT
				&& mutationTarget instanceof EntityMutationTarget
				&& ( (EntityMutationTarget) mutationTarget ).getIdentityInsertDelegate() != null ) {
			assert mutationTarget instanceof EntityMappingType;

			if ( numberOfOperations > 1 ) {
				return new MutationExecutorPostInsert( operationGroup, session );
			}

			return new MutationExecutorPostInsertSingleTable( operationGroup, session );
		}

		if ( numberOfOperations == 1 ) {
			final MutationOperation singleOperation = operationGroup.getSingleOperation();
			if ( singleOperation instanceof SelfExecutingUpdateOperation ) {
				return new MutationExecutorSingleSelfExecuting( (SelfExecutingUpdateOperation) singleOperation, session );
			}

			final PreparableMutationOperation jdbcOperation = (PreparableMutationOperation) singleOperation;
			final BatchKey batchKey = batchKeySupplier.getBatchKey();
			if ( jdbcOperation.canBeBatched( batchKey, batchSizeToUse ) ) {
				return new MutationExecutorSingleBatched( jdbcOperation, batchKey, batchSizeToUse, session );
			}

			return new MutationExecutorSingleNonBatched( jdbcOperation, session );
		}

		return new MutationExecutorStandard( operationGroup, batchKeySupplier, batchSizeToUse, session );
	}
}
