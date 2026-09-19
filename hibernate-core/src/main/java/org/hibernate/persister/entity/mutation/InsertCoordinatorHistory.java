/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.sql.SQLException;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
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
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.persister.entity.mutation.InsertCoordinatorStandard.getPropertiesToInsert;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Insert coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
@org.hibernate.Internal
public class InsertCoordinatorHistory extends AbstractMutationCoordinator implements InsertCoordinator {
	private final InsertCoordinator currentInsertCoordinator;
	private final EntityTableMapping identifierTableMapping;
	private final EntityTableMapping historyTableMapping;
	private final TemporalMapping temporalMapping;
	private final BasicBatchKey historyBatchKey;
	@Nullable
	private final MutationOperationGroup staticHistoryInsertGroup;

	public InsertCoordinatorHistory(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull InsertCoordinator currentInsertCoordinator) {
		super( entityPersister, factory );
		this.currentInsertCoordinator = currentInsertCoordinator;
		identifierTableMapping = entityPersister.getIdentifierTableMapping();
		temporalMapping = castNonNull( entityPersister.getTemporalMapping() );
		historyTableMapping =
				createAuxiliaryTableMapping( identifierTableMapping,
						entityPersister, temporalMapping.getTableName()
		);
		historyBatchKey = new BasicBatchKey( entityPersister.getEntityName() + "#HISTORY_INSERT" );
		staticHistoryInsertGroup = entityPersister.isDynamicInsert()
				? null
				: buildHistoryInsertGroup( entityPersister.getPropertyInsertability(), null, null );
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentInsertCoordinator.getStaticMutationOperationGroup();
	}

	@Nullable
	@Override
	protected BatchKey getBatchKey() {
		return historyBatchKey;
	}

	@Nullable
	@Override
	public GeneratedValues insert(@Nonnull Object entity, @Nonnull Object[] values, @Nonnull SharedSessionContractImplementor session) {
		final var generatedValues = currentInsertCoordinator.insert( entity, values, session );
		final Object id = resolveInsertedIdentifier( entity, null, generatedValues, session );
		insertHistoryRow( entity, id, values, session );
		return generatedValues;
	}

	@Nullable
	@Override
	public GeneratedValues insert(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull Object[] values,
			@Nonnull SharedSessionContractImplementor session) {
		final var generatedValues = currentInsertCoordinator.insert( entity, id, values, session );
		final Object resolvedId = resolveInsertedIdentifier( entity, id, generatedValues, session );
		insertHistoryRow( entity, resolvedId, values, session );
		return generatedValues;
	}

	private void insertHistoryRow(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] values,
			@Nonnull SharedSessionContractImplementor session) {
		final boolean dynamicInsert = entityPersister().isDynamicInsert();
		final boolean[] propertyInclusions = dynamicInsert
				? getPropertiesToInsert( entityPersister(), values )
				: entityPersister().getPropertyInsertability();
		final var operationGroup = dynamicInsert
				? buildHistoryInsertGroup( propertyInclusions, entity, session )
				: staticHistoryInsertGroup;

		final var mutationExecutor =
				mutationExecutorService.createExecutor( resolveBatchKeyAccess( dynamicInsert, session ),
						operationGroup, session );
		try {
			bindHistoryValues( id, values, propertyInclusions, session, mutationExecutor.getJdbcValueBindings() );
			mutationExecutor.execute( entity, null, null, InsertCoordinatorHistory::verifyOutcome, session );
		}
		finally {
			mutationExecutor.release();
		}
	}

	@Nonnull
	private MutationOperationGroup buildHistoryInsertGroup(
			@Nonnull boolean[] propertyInclusions,
			@Nullable Object entity,
			@Nullable SharedSessionContractImplementor session) {
		final var insertBuilder =
				new TableInsertBuilderStandard( entityPersister(), historyTableMapping, factory() );
		applyHistoryInsertDetails( insertBuilder, propertyInclusions, entity, session );
		final var tableMutation = insertBuilder.buildMutation();
		return singleOperation(
				new MutationGroupSingle( MutationType.INSERT, entityPersister(), tableMutation ),
				tableMutation.createMutationOperation( null, factory() )
		);
	}

	private void applyHistoryInsertDetails(
			@Nonnull TableInsertBuilderStandard insertBuilder,
			@Nonnull boolean[] propertyInclusions,
			@Nullable Object entity,
			@Nullable SharedSessionContractImplementor session) {
		final var attributeMappings = entityPersister().getAttributeMappings();
		for ( final int attributeIndex : identifierTableMapping.getAttributeIndexes() ) {
			final var attributeMapping = attributeMappings.get( attributeIndex );
			if ( propertyInclusions[attributeIndex] ) {
				attributeMapping.forEachInsertable( insertBuilder );
			}
			else {
				final var generator = attributeMapping.getGenerator();
				if ( generator != null && isValueGenerated( generator ) ) {
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
		insertBuilder.addColumnAssignment( temporalMapping.createStartingValueBinding( startingColumn ) );
		final var endingColumn = new ColumnReference( mutatingTable, temporalMapping.getEndingColumnMapping() );
		insertBuilder.addColumnAssignment( temporalMapping.createNullEndingValueBinding( endingColumn ) );

		identifierTableMapping.getKeyMapping().forEachKeyColumn( insertBuilder::addColumnAssignment );
	}

	private void addSqlGeneratedValue(
			@Nonnull TableInsertBuilderStandard insertBuilder,
			@Nonnull AttributeMapping attributeMapping,
			@Nonnull OnExecutionGenerator generator) {
		final boolean writePropertyValue = generator.writePropertyValue();
		final var columnValues =
				writePropertyValue
						? null
						: generator.getReferencedColumnValues( factory.getJdbcServices().getDialect() );
		attributeMapping.forEachSelectable( (j, mapping) ->
				insertBuilder.addValueColumn( writePropertyValue ? "?" : columnValues[j], mapping ) );
	}

	private void bindHistoryValues(
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

	private static boolean isValueGenerated(@Nullable Generator generator) {
		return generator != null
			&& generator.generatesOnInsert()
			&& generator.generatedOnExecution();
	}

	private boolean isValueGenerationInSql(@Nonnull Generator generator) {
		assert isValueGenerated( generator );
		return ( (OnExecutionGenerator) generator ).referenceColumnsInSql( dialect() );
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
}
