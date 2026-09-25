package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.sql.SQLException;

import org.hibernate.Internal;
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
import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Merge coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#SINGLE_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
@Internal
public class MergeCoordinatorTemporal extends AbstractTemporalUpdateCoordinator {
	private final TemporalMapping temporalMapping;
	private final MutationOperationGroup endingUpdateGroup;
	private final BasicBatchKey batchKey;
	private final UpdateCoordinator versionUpdateDelegate;
	private final String currentRowSelectSql;

	public MergeCoordinatorTemporal(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.temporalMapping = castNonNull( entityPersister.getTemporalMapping() );
		this.endingUpdateGroup = buildEndingUpdateGroup( entityPersister.getIdentifierTableMapping(), temporalMapping );
		this.batchKey = new BasicBatchKey( entityPersister.getEntityName() + "#TEMPORAL_MERGE" );
		this.versionUpdateDelegate = new MergeCoordinatorStandard( entityPersister, factory );
		this.currentRowSelectSql = buildCurrentRowSelect();
	}

	@Nonnull
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

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return endingUpdateGroup;
	}

	@Nullable
	@Override
	protected BasicBatchKey getBatchKey() {
		return batchKey;
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
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nullable Object oldVersion,
			@Nonnull SharedSessionContractImplementor session) {
		class Result implements OperationResultChecker {
			private boolean updated;
			@Override
			public boolean checkResult(@Nonnull PreparedStatementDetails statementDetails, int affectedRowCount, int batchPosition) {
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

	private boolean currentRowExists(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
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
	void bindVersionRestriction(@Nullable Object oldVersion, @Nonnull JdbcValueBindings jdbcValueBindings, @Nonnull String temporalTableName) {
		final var versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping != null && entityPersister().optimisticLockStyle().isVersion() ) {
			jdbcValueBindings.bindValue( oldVersion, versionMapping, ParameterUsage.RESTRICT );
		}
	}

	@Override
	public void forceVersionIncrement(
			@Nonnull Object id,
			@Nullable Object currentVersion,
			@Nonnull Object nextVersion,
			@Nonnull SharedSessionContractImplementor session) {
		versionUpdateDelegate.forceVersionIncrement( id, currentVersion, nextVersion, session );
	}
}
