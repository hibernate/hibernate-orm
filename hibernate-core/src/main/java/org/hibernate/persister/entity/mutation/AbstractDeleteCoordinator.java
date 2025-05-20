/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;

import static org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.identifiedResultsCheck;

/**
 * Template support for DeleteCoordinator implementations.  Mainly
 * centers around delegation via {@linkplain #generateOperationGroup}.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDeleteCoordinator
		extends AbstractMutationCoordinator
		implements DeleteCoordinator {
	private final BasicBatchKey batchKey;
	private final MutationOperationGroup staticOperationGroup;

	private MutationOperationGroup noVersionDeleteGroup;

	public AbstractDeleteCoordinator(
			AbstractEntityPersister entityPersister,
			SessionFactoryImplementor factory) {
		super( entityPersister, factory );

		this.batchKey = new BasicBatchKey( entityPersister.getEntityName() + "#DELETE" );
		this.staticOperationGroup = generateOperationGroup( "", null, true, null );
		if ( !entityPersister.isVersioned() ) {
			noVersionDeleteGroup = staticOperationGroup;
		}
	}

	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return staticOperationGroup;
	}

	@Override
	public BasicBatchKey getBatchKey() {
		return batchKey;
	}

	protected abstract MutationOperationGroup generateOperationGroup(
			Object rowId,
			Object[] loadedState,
			boolean applyVersion,
			SharedSessionContractImplementor session);

	@Override
	public void delete(
			Object entity,
			Object id,
			Object version,
			SharedSessionContractImplementor session) {
		boolean isImpliedOptimisticLocking = entityPersister().optimisticLockStyle().isAllOrDirty();

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final EntityEntry entry = persistenceContext.getEntry( entity );
		final Object[] loadedState = entry != null && isImpliedOptimisticLocking ? entry.getLoadedState() : null;
		final Object rowId = entry != null ? entry.getRowId() : null;

		if ( ( isImpliedOptimisticLocking && loadedState != null ) || ( rowId == null && entityPersister().hasRowId() ) ) {
			doDynamicDelete( entity, id, rowId, loadedState, session );
		}
		else {
			doStaticDelete( entity, id, rowId, entry == null ? null : entry.getLoadedState(), version, session );
		}
	}

	protected void doDynamicDelete(
			Object entity,
			Object id,
			Object rowId,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		final MutationOperationGroup operationGroup = generateOperationGroup( null, loadedState, true, session );
		final MutationExecutor mutationExecutor = executor( session, operationGroup );

		for ( int i = 0; i < operationGroup.getNumberOfOperations(); i++ ) {
			final MutationOperation mutation = operationGroup.getOperation( i );
			if ( mutation != null ) {
				final String tableName = mutation.getTableDetails().getTableName();
				mutationExecutor.getPreparedStatementDetails( tableName );
			}
		}

		applyDynamicDeleteTableDetails(
				id,
				rowId,
				loadedState,
				mutationExecutor,
				operationGroup,
				session
		);

		try {
			mutationExecutor.execute(
					entity,
					null,
					null,
					(statementDetails, affectedRowCount, batchPosition) -> identifiedResultsCheck(
							statementDetails,
							affectedRowCount,
							batchPosition,
							entityPersister(),
							id,
							factory()
					),
					session
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	protected void applyDynamicDeleteTableDetails(
			Object id,
			Object rowId,
			Object[] loadedState,
			MutationExecutor mutationExecutor,
			MutationOperationGroup operationGroup,
			SharedSessionContractImplementor session) {
		applyLocking( null, loadedState, mutationExecutor, session );
		applyId( id, null, mutationExecutor, operationGroup, session );
	}

	protected void applyLocking(
			Object version,
			Object[] loadedState,
			MutationExecutor mutationExecutor,
			SharedSessionContractImplementor session) {
		final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
		final OptimisticLockStyle optimisticLockStyle = entityPersister().optimisticLockStyle();
		switch ( optimisticLockStyle ) {
			case VERSION:
				applyVersionLocking( version, jdbcValueBindings );
				break;
			case ALL:
			case DIRTY:
				applyAllOrDirtyLocking( loadedState, session, jdbcValueBindings );
				break;
		}
	}

	private void applyAllOrDirtyLocking(
			Object[] loadedState,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		if ( loadedState != null ) {
			final AbstractEntityPersister persister = entityPersister();
			final boolean[] versionability = persister.getPropertyVersionability();
			for ( int attributeIndex = 0; attributeIndex < versionability.length; attributeIndex++ ) {
				final AttributeMapping attribute;
				// only makes sense to lock on singular attributes which are not excluded from optimistic locking
				if ( versionability[attributeIndex] && !( attribute = persister.getAttributeMapping( attributeIndex ) ).isPluralAttributeMapping() ) {
					final Object loadedValue = loadedState[attributeIndex];
					if ( loadedValue != null ) {
						final String mutationTableName = persister.getAttributeMutationTableName( attributeIndex );
						attribute.breakDownJdbcValues(
								loadedValue,
								0,
								jdbcValueBindings,
								mutationTableName,
								(valueIndex, bindings, tableName, jdbcValue, jdbcValueMapping) -> {
									if ( jdbcValue == null ) {
										// presumably the SQL was generated with `is null`
										return;
									}
									bindings.bindValue(
											jdbcValue,
											tableName,
											jdbcValueMapping.getSelectionExpression(),
											ParameterUsage.RESTRICT
									);
								},
								session
						);
					}
				}
			}
		}
	}

	private void applyVersionLocking(
			Object version,
			JdbcValueBindings jdbcValueBindings) {
		final AbstractEntityPersister persister = entityPersister();
		final EntityVersionMapping versionMapping = persister.getVersionMapping();
		if ( version != null && versionMapping != null ) {
			jdbcValueBindings.bindValue(
					version,
					persister.physicalTableNameForMutation( versionMapping ),
					versionMapping.getSelectionExpression(),
					ParameterUsage.RESTRICT
			);
		}
	}

	protected void applyId(
			Object id,
			Object rowId,
			MutationExecutor mutationExecutor,
			MutationOperationGroup operationGroup,
			SharedSessionContractImplementor session) {
		final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
		final EntityRowIdMapping rowIdMapping = entityPersister().getRowIdMapping();

		for ( int position = 0; position < operationGroup.getNumberOfOperations(); position++ ) {
			final MutationOperation jdbcMutation = operationGroup.getOperation( position );
			final EntityTableMapping tableDetails = (EntityTableMapping) jdbcMutation.getTableDetails();
			breakDownKeyJdbcValues( id, rowId, session, jdbcValueBindings, tableDetails );
			final PreparedStatementDetails statementDetails = mutationExecutor.getPreparedStatementDetails( tableDetails.getTableName() );
			if ( statementDetails != null ) {
				// force creation of the PreparedStatement
				//noinspection resource
				statementDetails.resolveStatement();
			}
		}
	}

	protected void doStaticDelete(
			Object entity,
			Object id,
			Object rowId,
			Object[] loadedState,
			Object version,
			SharedSessionContractImplementor session) {
		final boolean applyVersion;
		final MutationOperationGroup operationGroupToUse;
		if ( entity == null ) {
			applyVersion = false;
			operationGroupToUse = resolveNoVersionDeleteGroup( session );
		}
		else {
			applyVersion = true;
			operationGroupToUse = staticOperationGroup;
		}

		final MutationExecutor mutationExecutor = executor( session, operationGroupToUse );
		for ( int position = 0; position < staticOperationGroup.getNumberOfOperations(); position++ ) {
			final MutationOperation mutation = staticOperationGroup.getOperation( position );
			if ( mutation != null ) {
				mutationExecutor.getPreparedStatementDetails( mutation.getTableDetails().getTableName() );
			}
		}

		applyStaticDeleteTableDetails(
				id,
				rowId,
				loadedState,
				version,
				applyVersion,
				mutationExecutor,
				session
		);

		try {
			mutationExecutor.execute(
					entity,
					null,
					null,
					(statementDetails, affectedRowCount, batchPosition) -> identifiedResultsCheck(
							statementDetails,
							affectedRowCount,
							batchPosition,
							entityPersister(),
							id,
							factory()
					),
					session
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	protected void applyStaticDeleteTableDetails(
			Object id,
			Object rowId,
			Object[] loadedState,
			Object version,
			boolean applyVersion,
			MutationExecutor mutationExecutor,
			SharedSessionContractImplementor session) {
		if ( applyVersion ) {
			applyLocking( version, null, mutationExecutor, session );
		}

		final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
		bindPartitionColumnValueBindings( loadedState, session, jdbcValueBindings );

		applyId( id, rowId, mutationExecutor, staticOperationGroup, session );
	}

	private MutationExecutor executor(SharedSessionContractImplementor session, MutationOperationGroup group) {
		return mutationExecutorService.createExecutor( resolveBatchKeyAccess( false, session ), group, session );
	}

	protected MutationOperationGroup resolveNoVersionDeleteGroup(SharedSessionContractImplementor session) {
		if ( noVersionDeleteGroup == null ) {
			noVersionDeleteGroup = generateOperationGroup( "", null, false, session );
		}

		return noVersionDeleteGroup;
	}
}
