/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import java.sql.SQLException;

import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Merge coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#SINGLE_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
public class MergeCoordinatorTemporal extends AbstractTemporalUpdateCoordinator {
	private final TemporalMapping temporalMapping;
	private final MutationOperationGroup endingUpdateGroup;
	private final BasicBatchKey batchKey;
	private final UpdateCoordinator versionUpdateDelegate;
	private final String currentRowSelectSql;

	public MergeCoordinatorTemporal(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.temporalMapping = entityPersister.getTemporalMapping();
		this.endingUpdateGroup = buildEndingUpdateGroup( entityPersister.getIdentifierTableMapping(), temporalMapping );
		this.batchKey = new BasicBatchKey( entityPersister.getEntityName() + "#TEMPORAL_MERGE" );
		this.versionUpdateDelegate = new MergeCoordinatorStandard( entityPersister, factory );
		this.currentRowSelectSql = buildCurrentRowSelect();
	}

	private String buildCurrentRowSelect() {
		final var tableMapping = entityPersister().getIdentifierTableMapping();
		final var select = new SimpleSelect( factory() )
				.setTableName( tableMapping.getTableName() )
				.addColumn( "1" );
		for ( var column : tableMapping.getKeyMapping().getKeyColumns() ) {
			select.addRestriction( column.getColumnName() );
		}
		return select.addWhereToken( temporalMapping.getEndingColumnMapping().getSelectionExpression() + " is null" )
				.toStatementString();
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
			final boolean rowEnded = performRowEndUpdate( entity, id, rowId, oldVersion, session );
			if ( !rowEnded && currentRowExists( id, session ) ) {
				throw new StaleObjectStateException( entityPersister().getEntityName(), id );
			}
			return entityPersister().getInsertCoordinator().insert( entity, id, values, session );
		}
	}

	boolean performRowEndUpdate(
			Object entity,
			Object id,
			Object rowId,
			Object oldVersion,
			SharedSessionContractImplementor session) {
		class Result implements OperationResultChecker {
			private boolean updated;
			@Override
			public boolean checkResult(PreparedStatementDetails statementDetails, int affectedRowCount, int batchPosition) {
				updated = affectedRowCount > 0;
				return !updated
					|| resultCheck( id, statementDetails, affectedRowCount, batchPosition );
			}
		}
		final var resultChecker = new Result();
		performRowEndUpdate(
				entity,
				id,
				rowId,
				oldVersion,
				session,
				temporalMapping,
				endingUpdateGroup,
				entityPersister().physicalTableNameForMutation( temporalMapping.getEndingColumnMapping() ),
				resultChecker
		);
		return resultChecker.updated;
	}

	private boolean currentRowExists(Object id, SharedSessionContractImplementor session) {
		// A snapshot filtered by tenant cannot distinguish an absent row from a foreign row.
		// Only a missing current row permits insertion, regardless of tenant or application filters.
		final var coordinator = session.getJdbcCoordinator();
		final var resources = coordinator.getLogicalConnection().getResourceRegistry();
		try {
			final var statement = coordinator.getStatementPreparer().prepareStatement( currentRowSelectSql );
			try {
				entityPersister().getIdentifierType().nullSafeSet( statement, id, 1, session );
				final var resultSet = coordinator.getResultSetReturn().extract( statement, currentRowSelectSql );
				try {
					return resultSet.next();
				}
				finally {
					resources.release( resultSet, statement );
				}
			}
			finally {
				resources.release( statement );
				coordinator.afterStatementExecution();
			}
		}
		catch ( SQLException e ) {
			throw session.getJdbcServices().getSqlExceptionHelper()
					.convert( e, "Could not check current temporal row of " + entityPersister().getEntityName(), currentRowSelectSql );
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
