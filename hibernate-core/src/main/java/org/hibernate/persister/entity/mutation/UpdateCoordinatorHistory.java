/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.ColumnValuesTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Update coordinator for
 * {@link org.hibernate.cfg.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
public class UpdateCoordinatorHistory extends AbstractTemporalUpdateCoordinator {
	private final UpdateCoordinator currentUpdateCoordinator;
	private final EntityTableMapping identifierTableMapping;
	private final EntityTableMapping historyTableMapping;
	private final TemporalMapping temporalMapping;
	private final BasicBatchKey historyUpdateBatchKey;
	private final BasicBatchKey historyInsertBatchKey;
	private final MutationOperationGroup historyEndUpdateGroup;
	private final MutationOperationGroup historyInsertGroup;

	public UpdateCoordinatorHistory(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory,
			UpdateCoordinator currentUpdateCoordinator) {
		super( entityPersister, factory );
		this.currentUpdateCoordinator = currentUpdateCoordinator;
		this.identifierTableMapping = entityPersister.getIdentifierTableMapping();
		this.temporalMapping = entityPersister.getTemporalMapping();
		this.historyTableMapping =
				createAuxiliaryTableMapping( identifierTableMapping, entityPersister,
						temporalMapping.getTableName() );
		final String entityName = entityPersister.getEntityName();
		this.historyUpdateBatchKey = new BasicBatchKey( entityName + "#HISTORY_UPDATE" );
		this.historyInsertBatchKey = new BasicBatchKey( entityName + "#HISTORY_INSERT" );
		this.historyEndUpdateGroup = buildEndingUpdateGroup( historyTableMapping, temporalMapping );
		this.historyInsertGroup = buildHistoryInsertGroup( entityPersister.getPropertyInsertability() );
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
		if ( entityPersister()
				.excludedFromTemporalVersioning( dirtyAttributeIndexes, hasDirtyCollection ) ) {
			performHistoryExcludedUpdate(
					entity,
					id,
					rowId,
					values,
					oldVersion,
					incomingOldValues,
					dirtyAttributeIndexes,
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
					historyEndUpdateGroup,
					historyTableMapping.getTableName(),
					(statementDetails, affectedRowCount, batchPosition) ->
							resultCheck( id, statementDetails, affectedRowCount, batchPosition )

			);
			insertHistoryRow( id, values, session );
		}
		return generatedValues;
	}

	private void performHistoryExcludedUpdate(
			Object entity,
			Object id,
			Object rowId,
			Object[] values,
			Object oldVersion,
			Object[] incomingOldValues,
			int[] dirtyAttributeIndexes,
			SharedSessionContractImplementor session) {
		final var updateDetails =
				buildHistoryExcludedUpdateDetails( entity, rowId, dirtyAttributeIndexes, session );
		if ( updateDetails != null ) {
			final var mutationExecutor =
					mutationExecutorService.createExecutor( resolveBatchKeyAccess( true, session ),
							updateDetails.operationGroup, session );
			try {
				final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
				breakDownKeyJdbcValues( id, rowId, session, jdbcValueBindings, historyTableMapping );

				if ( updateDetails.applyVersionRestriction ) {
					final var versionMapping = entityPersister().getVersionMapping();
					jdbcValueBindings.bindValue(
							oldVersion,
							historyTableMapping.getTableName(),
							versionMapping.getSelectionExpression(),
							ParameterUsage.RESTRICT
					);
				}

				final var loadedState = incomingOldValues != null ? incomingOldValues : values;
				bindPartitionColumnValueBindings( loadedState, session, jdbcValueBindings );
				bindHistoryExcludedUpdateValues( values, updateDetails, session, jdbcValueBindings );

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
	}

	private HistoryExcludedUpdateDetails buildHistoryExcludedUpdateDetails(
			Object entity,
			Object rowId,
			int[] dirtyAttributeIndexes,
			SharedSessionContractImplementor session) {
		if ( dirtyAttributeIndexes == null || dirtyAttributeIndexes.length == 0 ) {
			return null;
		}

		final var attributeMappings = entityPersister().getAttributeMappings();
		final int attributeCount = attributeMappings.size();
		final boolean[] dirtyFlags = new boolean[attributeCount];
		for ( int dirtyAttributeIndex : dirtyAttributeIndexes ) {
			dirtyFlags[dirtyAttributeIndex] = true;
		}

		final var versionMapping = entityPersister().getVersionMapping();
		final var updateability =
				entityPersister().hasUninitializedLazyProperties( entity )
						? entityPersister().getNonLazyPropertyUpdateability()
						: entityPersister().getPropertyUpdateability();

		final var tableUpdateBuilder =
				new TableUpdateBuilderStandard<>( entityPersister(), historyTableMapping, factory() );
		final List<Integer> bindableAttributeIndexes = new ArrayList<>();
		boolean hasValues = false;

		for ( final int attributeIndex : identifierTableMapping.getAttributeIndexes() ) {
			final var attributeMapping = attributeMappings.get( attributeIndex );
			if ( !(attributeMapping instanceof PluralAttributeMapping) ) {
				if ( entityPersister().isPropertyTemporalExcluded( attributeIndex ) ) {
					final var generator = attributeMapping.getGenerator();
					final boolean generatedInSql = needsUpdateValueGeneration( entity, session, generator );
					final boolean include =
							generatedInSql
								|| isGeneratedBeforeExecution( entity, session, generator )
								|| dirtyFlags[attributeIndex] && updateability[attributeIndex];

					if ( include ) {
						if ( generatedInSql ) {
							final var onExecutionGenerator = (OnExecutionGenerator) generator;
							addSqlGeneratedValue( tableUpdateBuilder, attributeMapping, onExecutionGenerator );
							hasValues = true;
							if ( onExecutionGenerator.writePropertyValue() ) {
								bindableAttributeIndexes.add( attributeIndex );
							}
						}
						else {
							attributeMapping.forEachUpdatable( tableUpdateBuilder );
							hasValues = true;
							bindableAttributeIndexes.add( attributeIndex );
						}
					}
				}
			}
		}

		if ( hasValues ) {
			applyKeyRestriction( rowId, entityPersister(), tableUpdateBuilder, historyTableMapping );
			applyCurrentRowRestriction( tableUpdateBuilder );
			applyPartitionKeyRestriction( tableUpdateBuilder );
			applyOptimisticLocking( tableUpdateBuilder );

			return new HistoryExcludedUpdateDetails(
					createMutationOperationGroup( tableUpdateBuilder ),
					toIntArray( bindableAttributeIndexes ),
					entityPersister().optimisticLockStyle().isVersion()
							&& versionMapping != null
			);
		}
		else {
			return null;
		}

	}

	private static boolean isGeneratedBeforeExecution(
			Object entity, SharedSessionContractImplementor session, Generator generator) {
		return generator != null
			&& generator.generatesOnUpdate()
			&& generator.generatedBeforeExecution( entity, session );
	}

	private void bindHistoryExcludedUpdateValues(
			Object[] values,
			HistoryExcludedUpdateDetails updateDetails,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attributeMappings = entityPersister().getAttributeMappings();
		for ( final int attributeIndex : updateDetails.attributeIndexes ) {
			final var attributeMapping = attributeMappings.get( attributeIndex );
			if ( !(attributeMapping instanceof PluralAttributeMapping) ) {
				attributeMapping.decompose(
						values[attributeIndex],
						0,
						jdbcValueBindings,
						historyTableMapping,
						(valueIndex, bindings, table, jdbcValue, selectableMapping) -> {
							if ( selectableMapping.isUpdateable() && !selectableMapping.isFormula() ) {
								bindings.bindValue(
										jdbcValue,
										table.getTableName(),
										selectableMapping.getSelectionExpression(),
										ParameterUsage.SET
								);
							}
						},
						session
				);
			}
		}
	}

	private void applyCurrentRowRestriction(TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var endingColumnReference =
				new ColumnReference( tableUpdateBuilder.getMutatingTable(), temporalMapping.getEndingColumnMapping() );
		tableUpdateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumnReference ) );
	}

	@Override
	void bindVersionRestriction(Object oldVersion, JdbcValueBindings jdbcValueBindings, String temporalTableName) {
		final var versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping != null && entityPersister().optimisticLockStyle().isVersion() ) {
			jdbcValueBindings.bindValue(
					oldVersion,
					temporalTableName,
					versionMapping.getSelectionExpression(),
					ParameterUsage.RESTRICT
			);
		}
	}

	private void insertHistoryRow(
			Object id,
			Object[] values,
			SharedSessionContractImplementor session) {
		final var mutationExecutor =
				mutationExecutorService.createExecutor( () -> historyInsertBatchKey, historyInsertGroup, session );
		try {
			bindHistoryInsertValues( id, values, entityPersister().getPropertyInsertability(), session,
					mutationExecutor.getJdbcValueBindings() );
			mutationExecutor.execute( id, null, null,
					UpdateCoordinatorHistory::verifyOutcome, session );
		}
		finally {
			mutationExecutor.release();
		}
	}

	private MutationOperationGroup buildHistoryInsertGroup(boolean[] propertyInclusions) {
		final var insertBuilder =
				new TableInsertBuilderStandard( entityPersister(), historyTableMapping, factory() );
		applyHistoryInsertDetails( insertBuilder, propertyInclusions );
		final var tableMutation = insertBuilder.buildMutation();
		return singleOperation(
				new MutationGroupSingle( MutationType.INSERT, entityPersister(), tableMutation ),
				tableMutation.createMutationOperation( null, factory() )
		);
	}

	private void applyHistoryInsertDetails(
			TableInsertBuilderStandard insertBuilder,
			boolean[] propertyInclusions) {
		final var attributeMappings = entityPersister().getAttributeMappings();
		for ( final int attributeIndex : identifierTableMapping.getAttributeIndexes() ) {
			final var attributeMapping = attributeMappings.get( attributeIndex );
			if ( propertyInclusions[attributeIndex] ) {
				attributeMapping.forEachInsertable( insertBuilder );
			}
			else {
				final var generator = attributeMapping.getGenerator();
				if ( isValueGeneratedOnInsert( generator ) ) {
//					if ( session != null && generator.generatedBeforeExecution( entity, session ) ) {
//						propertyInclusions[attributeIndex] = true;
//						attributeMapping.forEachInsertable( insertBuilder );
//					}
//					else
					if ( isValueGenerationInSql( generator ) ) {
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
			ColumnValuesTableMutationBuilder<?> updateBuilder,
			AttributeMapping attributeMapping,
			OnExecutionGenerator generator) {
		final boolean writePropertyValue = generator.writePropertyValue();
		final var columnValues =
				writePropertyValue
						? null
						: generator.getReferencedColumnValues( factory.getJdbcServices().getDialect() );
		attributeMapping.forEachSelectable( (j, mapping) ->
				updateBuilder.addValueColumn( writePropertyValue ? "?" : columnValues[j], mapping ) );
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

		final var attributeMappings = entityPersister().getAttributeMappings();
		for ( final int attributeIndex : identifierTableMapping.getAttributeIndexes() ) {
			if ( propertyInclusions[attributeIndex] ) {
				final var attributeMapping = attributeMappings.get( attributeIndex );
				if ( !(attributeMapping instanceof PluralAttributeMapping) ) {
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
			}
		}

		if ( TemporalMutationHelper.isUsingParameters( session ) ) {
			jdbcValueBindings.bindValue(
					session.getCurrentTransactionIdentifier(),
					historyTableName,
					temporalMapping.getStartingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	private static boolean isValueGeneratedOnInsert(Generator generator) {
		return generator != null
			&& generator.generatesOnInsert()
			&& generator.generatedOnExecution();
	}

	private static boolean isValueGeneratedOnUpdate(Generator generator) {
		return generator != null
			&& generator.generatesOnUpdate()
			&& generator.generatedOnExecution();
	}

	private boolean isValueGenerationInSql(Generator generator) {
		assert isValueGeneratedOnInsert( generator );
		return ( (OnExecutionGenerator) generator ).referenceColumnsInSql( dialect() );
	}

	private boolean isUpdateValueGenerationInSql(Generator generator) {
		assert isValueGeneratedOnUpdate( generator );
		return ( (OnExecutionGenerator) generator ).referenceColumnsInSql( dialect() );
	}

	private boolean needsUpdateValueGeneration(
			Object entity,
			SharedSessionContractImplementor session,
			Generator generator) {
		return isValueGeneratedOnUpdate( generator )
			&& (session == null && generator.generatedOnExecution() || generator.generatedOnExecution( entity, session ) )
			&& isUpdateValueGenerationInSql( generator );
	}

	private static int[] toIntArray(List<Integer> values) {
		final int[] result = new int[values.size()];
		for ( int i = 0; i < values.size(); i++ ) {
			result[i] = values.get( i );
		}
		return result;
	}

	private static final class HistoryExcludedUpdateDetails {
		private final MutationOperationGroup operationGroup;
		private final int[] attributeIndexes;
		private final boolean applyVersionRestriction;

		private HistoryExcludedUpdateDetails(
				MutationOperationGroup operationGroup,
				int[] attributeIndexes,
				boolean applyVersionRestriction) {
			this.operationGroup = operationGroup;
			this.attributeIndexes = attributeIndexes;
			this.applyVersionRestriction = applyVersionRestriction;
		}
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
