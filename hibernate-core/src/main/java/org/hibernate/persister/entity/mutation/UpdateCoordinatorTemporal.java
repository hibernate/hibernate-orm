/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;


/**
 * Update coordinator for
 * {@link org.hibernate.cfg.TemporalTableStrategy#SINGLE_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
public class UpdateCoordinatorTemporal extends AbstractTemporalUpdateCoordinator {
	private final TemporalMapping temporalMapping;
	private final MutationOperationGroup endingUpdateGroup;
	private final BasicBatchKey batchKey;
	private final UpdateCoordinator versionUpdateDelegate;

	public UpdateCoordinatorTemporal(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.temporalMapping = entityPersister.getTemporalMapping();
		this.endingUpdateGroup = buildEndingUpdateGroup( entityPersister.getIdentifierTableMapping(), temporalMapping );
		this.batchKey = new BasicBatchKey( entityPersister.getEntityName() + "#TEMPORAL_UPDATE" );
		this.versionUpdateDelegate = new UpdateCoordinatorStandard( entityPersister, factory );
	}

	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return endingUpdateGroup;
	}

	@Override
	protected BasicBatchKey getBatchKey() {
		return batchKey;
	}

	@Override
	public GeneratedValues update(
			Object entity,
			Object id,
			Object rowId,
			Object[] values,
			Object oldVersion,
			Object[] incomingOldValues,
			int[] dirtyAttributeIndexes,
			boolean hasDirtyCollection,
			SharedSessionContractImplementor session) {
		if ( entityPersister()
				.excludedFromTemporalVersioning( dirtyAttributeIndexes, hasDirtyCollection ) ) {
			return versionUpdateDelegate.update(
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
		}
		else {
			performRowEndUpdate(
					entity,
					id,
					rowId,
					oldVersion,
					session,
					temporalMapping,
					endingUpdateGroup,
					entityPersister()
							.physicalTableNameForMutation( temporalMapping.getEndingColumnMapping() )
			);
			return entityPersister().getInsertCoordinator().insert( entity, id, values, session );
		}
	}

	@Override
	void bindVersionRestriction(Object oldVersion, JdbcValueBindings jdbcValueBindings, String temporalTableName) {
		final var versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping != null && entityPersister().optimisticLockStyle().isVersion() ) {
			jdbcValueBindings.bindValue( oldVersion, versionMapping, ParameterUsage.RESTRICT );
		}
	}

	@Override
	public void forceVersionIncrement(
			Object id,
			Object currentVersion,
			Object nextVersion,
			SharedSessionContractImplementor session) {
		versionUpdateDelegate.forceVersionIncrement( id, currentVersion, nextVersion, session );
	}
}
