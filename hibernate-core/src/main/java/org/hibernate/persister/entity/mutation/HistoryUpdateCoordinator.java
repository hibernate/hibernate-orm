/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import java.sql.SQLException;

import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.identifiedResultsCheck;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Update coordinator for HISTORY temporal strategy.
 */
public class HistoryUpdateCoordinator extends AbstractMutationCoordinator implements UpdateCoordinator {
	private final UpdateCoordinator currentUpdateCoordinator;
	private final EntityTableMapping identifierTableMapping;
	private final EntityTableMapping historyTableMapping;
	private final TemporalMapping temporalMapping;
	private final BasicBatchKey historyUpdateBatchKey;
	private final BasicBatchKey historyInsertBatchKey;
	private final MutationOperationGroup historyEndUpdateGroup;
	private final MutationOperationGroup historyInsertGroup;

	public HistoryUpdateCoordinator(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory,
			UpdateCoordinator currentUpdateCoordinator) {
		super( entityPersister, factory );
		this.currentUpdateCoordinator = currentUpdateCoordinator;
		this.identifierTableMapping = entityPersister.getIdentifierTableMapping();
		this.temporalMapping = entityPersister.getTemporalMapping();
		this.historyTableMapping = HistoryTableMappingHelper.createHistoryTableMapping(
				identifierTableMapping,
				entityPersister,
				temporalMapping.getTableName()
		);
		this.historyUpdateBatchKey = new BasicBatchKey( entityPersister.getEntityName() + "#HISTORY_UPDATE" );
		this.historyInsertBatchKey = new BasicBatchKey( entityPersister.getEntityName() + "#HISTORY_INSERT" );
		this.historyEndUpdateGroup = buildHistoryEndUpdateGroup();
		this.historyInsertGroup = buildHistoryInsertGroup( entityPersister.getPropertyInsertability(), null, null );
	}

	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentUpdateCoordinator.getStaticMutationOperationGroup();
	}

	@Override
	protected BasicBatchKey getBatchKey() {
		return historyUpdateBatchKey;
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
		performHistoryEndingUpdate( entity, id, rowId, oldVersion, session );
		insertHistoryRow( id, values, session );
		return generatedValues;
	}

	private void performHistoryEndingUpdate(
			Object entity,
			Object id,
			Object rowId,
			Object oldVersion,
			SharedSessionContractImplementor session) {
		final MutationExecutor mutationExecutor =
				mutationExecutorService.createExecutor( resolveBatchKeyAccess( false, session ), historyEndUpdateGroup, session );
		try {
			final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			for ( int i = 0; i < historyEndUpdateGroup.getNumberOfOperations(); i++ ) {
				final var operation = historyEndUpdateGroup.getOperation( i );
				breakDownKeyJdbcValues( id, rowId, session, jdbcValueBindings, (EntityTableMapping) operation.getTableDetails() );
			}

			final EntityVersionMapping versionMapping = entityPersister().getVersionMapping();
			if ( versionMapping != null && entityPersister().optimisticLockStyle().isVersion() ) {
				jdbcValueBindings.bindValue(
						oldVersion,
						historyTableMapping.getTableName(),
						versionMapping.getSelectionExpression(),
						ParameterUsage.RESTRICT
				);
			}

			if ( TemporalMutationHelper.isUsingParameters( session ) ) {
				jdbcValueBindings.bindValue(
						session.getTransactionStartInstant(),
						historyTableMapping.getTableName(),
						temporalMapping.getEndingColumnMapping().getSelectionExpression(),
						ParameterUsage.SET
				);
			}

			mutationExecutor.execute(
					entity,
					null,
					null,
					(statementDetails, affectedRowCount, batchPosition) ->
							resultCheck( id, statementDetails, affectedRowCount, batchPosition ),
					session,
					staleStateException -> staleObjectStateException( id, staleStateException )
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	private void insertHistoryRow(
			Object id,
			Object[] values,
			SharedSessionContractImplementor session) {
		final MutationExecutor mutationExecutor =
				mutationExecutorService.createExecutor( () -> historyInsertBatchKey, historyInsertGroup, session );
		try {
			bindHistoryInsertValues( id, values, entityPersister().getPropertyInsertability(), session,
					mutationExecutor.getJdbcValueBindings() );
			mutationExecutor.execute( id, null, null, HistoryUpdateCoordinator::verifyOutcome, session );
		}
		finally {
			mutationExecutor.release();
		}
	}

	private MutationOperationGroup buildHistoryEndUpdateGroup() {
		final TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder =
				new TableUpdateBuilderStandard<>( entityPersister(), historyTableMapping, factory() );

		applyKeyRestriction( null, entityPersister(), tableUpdateBuilder, historyTableMapping );
		applyTemporalEnding( tableUpdateBuilder );
		applyPartitionKeyRestriction( tableUpdateBuilder );
		applyOptimisticLocking( tableUpdateBuilder );

		final var tableMutation = tableUpdateBuilder.buildMutation();
		final MutationGroupSingle mutationGroup = new MutationGroupSingle(
				MutationType.UPDATE,
				entityPersister(),
				tableMutation
		);

		return singleOperation( mutationGroup, tableMutation.createMutationOperation( null, factory() ) );
	}

	private void applyTemporalEnding(TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var endingColumnReference =
				new ColumnReference( tableUpdateBuilder.getMutatingTable(), temporalMapping.getEndingColumnMapping() );
		tableUpdateBuilder.addValueColumn( temporalMapping.createEndingValueBinding( endingColumnReference ) );
		tableUpdateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumnReference ) );
	}

	private void applyPartitionKeyRestriction(TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var persister = entityPersister();
		if ( persister.hasPartitionedSelectionMapping() ) {
			final var attributeMappings = persister.getAttributeMappings();
			for ( int m = 0; m < attributeMappings.size(); m++ ) {
				final var attributeMapping = attributeMappings.get( m );
				final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
				for ( int i = 0; i < jdbcTypeCount; i++ ) {
					final var selectableMapping = attributeMapping.getSelectable( i );
					if ( selectableMapping.isPartitioned() ) {
						tableUpdateBuilder.addKeyRestrictionLeniently( selectableMapping );
					}
				}
			}
		}
	}

	private void applyOptimisticLocking(TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		if ( entityPersister().optimisticLockStyle() == OptimisticLockStyle.VERSION
				&& entityPersister().getVersionMapping() != null ) {
			tableUpdateBuilder.addOptimisticLockRestriction( entityPersister().getVersionMapping() );
		}
	}

	private MutationOperationGroup buildHistoryInsertGroup(
			boolean[] propertyInclusions,
			Object entity,
			SharedSessionContractImplementor session) {
		final var insertBuilder = new TableInsertBuilderStandard( entityPersister(), historyTableMapping, factory() );
		applyHistoryInsertDetails( insertBuilder, propertyInclusions, entity, session );
		final TableInsert tableMutation = insertBuilder.buildMutation();
		final MutationGroupSingle mutationGroup = new MutationGroupSingle(
				MutationType.INSERT,
				entityPersister(),
				tableMutation
		);
		return singleOperation( mutationGroup, tableMutation.createMutationOperation( null, factory() ) );
	}

	private void applyHistoryInsertDetails(
			TableInsertBuilderStandard insertBuilder,
			boolean[] propertyInclusions,
			Object entity,
			SharedSessionContractImplementor session) {
		final AttributeMappingsList attributeMappings = entityPersister().getAttributeMappings();
		final int[] attributeIndexes = identifierTableMapping.getAttributeIndexes();
		for ( int i = 0; i < attributeIndexes.length; i++ ) {
			final int attributeIndex = attributeIndexes[i];
			final var attributeMapping = attributeMappings.get( attributeIndex );
			if ( propertyInclusions[attributeIndex] ) {
				attributeMapping.forEachInsertable( insertBuilder );
			}
			else {
				final var generator = attributeMapping.getGenerator();
				if ( isValueGenerated( generator ) ) {
					if ( session != null && generator.generatedBeforeExecution( entity, session ) ) {
						propertyInclusions[attributeIndex] = true;
						attributeMapping.forEachInsertable( insertBuilder );
					}
					else if ( isValueGenerationInSql( generator ) ) {
						addSqlGeneratedValue( insertBuilder, attributeMapping, (OnExecutionGenerator) generator );
					}
				}
			}
		}

		final var mutatingTable = insertBuilder.getMutatingTable();
		final var startingColumn = new ColumnReference( mutatingTable, temporalMapping.getStartingColumnMapping() );
		insertBuilder.addValueColumn( temporalMapping.createStartingValueBinding( startingColumn ) );
		final var endingColumn = new ColumnReference( mutatingTable, temporalMapping.getEndingColumnMapping() );
		insertBuilder.addValueColumn( temporalMapping.createNullEndingValueBinding( endingColumn ) );

		identifierTableMapping.getKeyMapping().forEachKeyColumn( insertBuilder::addKeyColumn );
	}

	private void addSqlGeneratedValue(
			TableInsertBuilderStandard insertBuilder,
			AttributeMapping attributeMapping,
			OnExecutionGenerator generator) {
		final boolean writePropertyValue = generator.writePropertyValue();
		final String[] columnValues = writePropertyValue
				? null
				: generator.getReferencedColumnValues( factory.getJdbcServices().getDialect() );
		attributeMapping.forEachSelectable( (j, mapping) -> {
			insertBuilder.addValueColumn( writePropertyValue ? "?" : columnValues[j], mapping );
		} );
	}

	private void bindHistoryInsertValues(
			Object id,
			Object[] values,
			boolean[] propertyInclusions,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final String historyTableName = historyTableMapping.getTableName();
		historyTableMapping.getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> jdbcValueBindings.bindValue(
						jdbcValue,
						historyTableName,
						columnMapping.getColumnName(),
						ParameterUsage.SET
				),
				session
		);

		final AttributeMappingsList attributeMappings = entityPersister().getAttributeMappings();
		final int[] attributeIndexes = identifierTableMapping.getAttributeIndexes();
		for ( int i = 0; i < attributeIndexes.length; i++ ) {
			final int attributeIndex = attributeIndexes[i];
			if ( !propertyInclusions[attributeIndex] ) {
				continue;
			}
			final AttributeMapping attributeMapping = attributeMappings.get( attributeIndex );
			if ( attributeMapping instanceof PluralAttributeMapping ) {
				continue;
			}
			attributeMapping.decompose(
					values[attributeIndex],
					0,
					jdbcValueBindings,
					null,
					(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
						if ( selectableMapping.isInsertable() && !selectableMapping.isFormula() ) {
							bindings.bindValue(
									jdbcValue,
									historyTableName,
									selectableMapping.getSelectionExpression(),
									ParameterUsage.SET
							);
						}
					},
					session
			);
		}

		if ( TemporalMutationHelper.isUsingParameters( session ) ) {
			jdbcValueBindings.bindValue(
					session.getTransactionStartInstant(),
					historyTableName,
					temporalMapping.getStartingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	private static boolean isValueGenerated(Generator generator) {
		return generator != null
			&& generator.generatesOnInsert()
			&& generator.generatedOnExecution();
	}

	private boolean isValueGenerationInSql(Generator generator) {
		assert isValueGenerated( generator );
		return ( (OnExecutionGenerator) generator ).referenceColumnsInSql( dialect() );
	}

	private boolean resultCheck(
			Object id,
			PreparedStatementDetails statementDetails,
			int affectedRowCount,
			int batchPosition) {
		return identifiedResultsCheck(
				statementDetails,
				affectedRowCount,
				batchPosition,
				entityPersister(),
				id,
				factory()
		);
	}

	private StaleObjectStateException staleObjectStateException(Object id, StaleStateException cause) {
		return new StaleObjectStateException( entityPersister().getEntityName(), id, cause );
	}

	private static boolean verifyOutcome(
			PreparedStatementDetails statementDetails,
			int affectedRowCount,
			int batchPosition) throws SQLException {
		statementDetails.getExpectation().verifyOutcome(
				affectedRowCount,
				statementDetails.getStatement(),
				batchPosition,
				statementDetails.getSqlString()
		);
		return true;
	}

	@Override
	public void forceVersionIncrement(
			Object id,
			Object currentVersion,
			Object nextVersion,
			SharedSessionContractImplementor session) {
		currentUpdateCoordinator.forceVersionIncrement( id, currentVersion, nextVersion, session );
	}
}
