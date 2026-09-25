package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
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
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.ast.spi.model.builder.AssigningTableMutationBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilderStandard;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Update coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
@Internal
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
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull UpdateCoordinator currentUpdateCoordinator) {
		super( entityPersister, factory );
		this.currentUpdateCoordinator = currentUpdateCoordinator;
		this.identifierTableMapping = entityPersister.getIdentifierTableMapping();
		this.temporalMapping = castNonNull( entityPersister.getTemporalMapping() );
		this.historyTableMapping =
				createAuxiliaryTableMapping( identifierTableMapping, entityPersister,
						temporalMapping.getTableName() );
		final String entityName = entityPersister.getEntityName();
		this.historyUpdateBatchKey = new BasicBatchKey( entityName + "#HISTORY_UPDATE" );
		this.historyInsertBatchKey = new BasicBatchKey( entityName + "#HISTORY_INSERT" );
		this.historyEndUpdateGroup = buildEndingUpdateGroup( historyTableMapping, temporalMapping );
		this.historyInsertGroup = buildHistoryInsertGroup( entityPersister.getPropertyInsertability() );
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentUpdateCoordinator.getStaticMutationOperationGroup();
	}

	@Nullable
	@Override
	protected BasicBatchKey getBatchKey() {
		return historyUpdateBatchKey;
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
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull Object[] values,
			@Nullable Object oldVersion,
			@Nullable Object[] incomingOldValues,
			@Nullable int[] dirtyAttributeIndexes,
			@Nonnull SharedSessionContractImplementor session) {
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

	@Nullable
	private HistoryExcludedUpdateDetails buildHistoryExcludedUpdateDetails(
			@Nonnull Object entity,
			@Nullable Object rowId,
			@Nullable int[] dirtyAttributeIndexes,
			@Nonnull SharedSessionContractImplementor session) {
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
			@Nonnull Object entity, @Nonnull SharedSessionContractImplementor session, @Nullable Generator generator) {
		return generator != null
			&& generator.generatesOnUpdate()
			&& generator.generatedBeforeExecution( entity, session );
	}

	private void bindHistoryExcludedUpdateValues(
			@Nonnull Object[] values,
			@Nonnull HistoryExcludedUpdateDetails updateDetails,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
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

	private void applyCurrentRowRestriction(@Nonnull TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var endingColumnReference =
				new ColumnReference( tableUpdateBuilder.getMutatingTable(), temporalMapping.getEndingColumnMapping() );
		tableUpdateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumnReference ) );
	}

	@Override
	void bindVersionRestriction(@Nullable Object oldVersion, @Nonnull JdbcValueBindings jdbcValueBindings, @Nonnull String temporalTableName) {
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
			@Nonnull Object id,
			@Nonnull Object[] values,
			@Nonnull SharedSessionContractImplementor session) {
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

	@Nonnull
	private MutationOperationGroup buildHistoryInsertGroup(@Nonnull boolean[] propertyInclusions) {
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
			@Nonnull TableInsertBuilderStandard insertBuilder,
			@Nonnull boolean[] propertyInclusions) {
		final var attributeMappings = entityPersister().getAttributeMappings();
		for ( final int attributeIndex : identifierTableMapping.getAttributeIndexes() ) {
			final var attributeMapping = attributeMappings.get( attributeIndex );
			if ( propertyInclusions[attributeIndex] ) {
				attributeMapping.forEachInsertable( insertBuilder );
			}
			else {
				final var generator = attributeMapping.getGenerator();
				if ( generator != null && isValueGeneratedOnInsert( generator ) ) {
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
		insertBuilder.addColumnAssignment( temporalMapping.createStartingValueBinding( startingColumn ) );
		final var endingColumn = new ColumnReference( mutatingTable, temporalMapping.getEndingColumnMapping() );
		insertBuilder.addColumnAssignment( temporalMapping.createNullEndingValueBinding( endingColumn ) );

		identifierTableMapping.getKeyMapping().forEachKeyColumn( insertBuilder::addColumnAssignment );
	}

	private void addSqlGeneratedValue(
			@Nonnull AssigningTableMutationBuilder<?> updateBuilder,
			@Nonnull AttributeMapping attributeMapping,
			@Nonnull OnExecutionGenerator generator) {
		final boolean writePropertyValue = generator.writePropertyValue();
		final var columnValues =
				writePropertyValue
						? null
						: generator.getReferencedColumnValues( factory.getJdbcServices().getDialect() );
		attributeMapping.forEachSelectable( (j, mapping) ->
				updateBuilder.addColumnAssignment( mapping, writePropertyValue ? "?" : columnValues[j] ) );
	}

	private void bindHistoryInsertValues(
			@Nonnull Object id,
			@Nonnull Object[] values,
			@Nonnull boolean[] propertyInclusions,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
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
					session.getCurrentChangesetIdentifier(),
					historyTableName,
					temporalMapping.getStartingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	private static boolean isValueGeneratedOnInsert(@Nullable Generator generator) {
		return generator != null
			&& generator.generatesOnInsert()
			&& generator.generatedOnExecution();
	}

	private static boolean isValueGeneratedOnUpdate(@Nullable Generator generator) {
		return generator != null
			&& generator.generatesOnUpdate()
			&& generator.generatedOnExecution();
	}

	private boolean isValueGenerationInSql(@Nonnull Generator generator) {
		assert isValueGeneratedOnInsert( generator );
		return ( (OnExecutionGenerator) generator ).referenceColumnsInSql( dialect() );
	}

	private boolean isUpdateValueGenerationInSql(@Nonnull Generator generator) {
		assert isValueGeneratedOnUpdate( generator );
		return ( (OnExecutionGenerator) generator ).referenceColumnsInSql( dialect() );
	}

	private boolean needsUpdateValueGeneration(
			@Nonnull Object entity,
			@Nullable SharedSessionContractImplementor session,
			@Nullable Generator generator) {
		return generator != null && isValueGeneratedOnUpdate( generator )
			&& (session == null && generator.generatedOnExecution() || generator.generatedOnExecution( entity, session ) )
			&& isUpdateValueGenerationInSql( generator );
	}

	@Nonnull
	private static int[] toIntArray(@Nonnull List<Integer> values) {
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
				@Nonnull MutationOperationGroup operationGroup,
				@Nonnull int[] attributeIndexes,
				boolean applyVersionRestriction) {
			this.operationGroup = operationGroup;
			this.attributeIndexes = attributeIndexes;
			this.applyVersionRestriction = applyVersionRestriction;
		}
	}

	private static boolean verifyOutcome(
			@Nonnull PreparedStatementDetails statementDetails,
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
			@Nonnull Object id,
			@Nullable Object currentVersion,
			@Nonnull Object nextVersion,
			@Nonnull SharedSessionContractImplementor session) {
		currentUpdateCoordinator.forceVersionIncrement( id, currentVersion, nextVersion, session );
	}
}
