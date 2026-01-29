/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

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
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Insert coordinator for
 * {@link org.hibernate.cfg.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
public class InsertCoordinatorHistory extends AbstractMutationCoordinator implements InsertCoordinator {
	private final InsertCoordinatorStandard currentInsertCoordinator;
	private final EntityTableMapping identifierTableMapping;
	private final EntityTableMapping historyTableMapping;
	private final TemporalMapping temporalMapping;
	private final BasicBatchKey historyBatchKey;
	private final MutationOperationGroup staticHistoryInsertGroup;

	public InsertCoordinatorHistory(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.currentInsertCoordinator = new InsertCoordinatorStandard( entityPersister, factory );
		this.identifierTableMapping = entityPersister.getIdentifierTableMapping();
		this.temporalMapping = entityPersister.getTemporalMapping();
		this.historyTableMapping = HistoryTableMappingHelper.createHistoryTableMapping(
				identifierTableMapping,
				entityPersister,
				temporalMapping.getTableName()
		);
		this.historyBatchKey = new BasicBatchKey( entityPersister.getEntityName() + "#HISTORY_INSERT" );
		this.staticHistoryInsertGroup = entityPersister.isDynamicInsert()
				? null
				: buildHistoryInsertGroup( entityPersister.getPropertyInsertability(), null, null );
	}

	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentInsertCoordinator.getStaticMutationOperationGroup();
	}

	@Override
	protected BatchKey getBatchKey() {
		return historyBatchKey;
	}

	@Override
	public GeneratedValues insert(Object entity, Object[] values, SharedSessionContractImplementor session) {
		final var generatedValues = currentInsertCoordinator.insert( entity, values, session );
		final Object id = entityPersister().getIdentifier( entity, session );
		insertHistoryRow( entity, id, values, session );
		return generatedValues;
	}

	@Override
	public GeneratedValues insert(
			Object entity,
			Object id,
			Object[] values,
			SharedSessionContractImplementor session) {
		final var generatedValues = currentInsertCoordinator.insert( entity, id, values, session );
		final Object resolvedId = id == null ? entityPersister().getIdentifier( entity, session ) : id;
		insertHistoryRow( entity, resolvedId, values, session );
		return generatedValues;
	}

	private void insertHistoryRow(
			Object entity,
			Object id,
			Object[] values,
			SharedSessionContractImplementor session) {
		final boolean dynamicInsert = entityPersister().isDynamicInsert();
		final boolean[] propertyInclusions = dynamicInsert
				? currentInsertCoordinator.getPropertiesToInsert( values )
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

	private MutationOperationGroup buildHistoryInsertGroup(
			boolean[] propertyInclusions,
			Object entity,
			SharedSessionContractImplementor session) {
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
			TableInsertBuilderStandard insertBuilder,
			boolean[] propertyInclusions,
			Object entity,
			SharedSessionContractImplementor session) {
		final var attributeMappings = entityPersister().getAttributeMappings();
		for ( final int attributeIndex : identifierTableMapping.getAttributeIndexes() ) {
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
		final var columnValues =
				writePropertyValue
						? null
						: generator.getReferencedColumnValues( factory.getJdbcServices().getDialect() );
		attributeMapping.forEachSelectable( (j, mapping) ->
				insertBuilder.addValueColumn( writePropertyValue ? "?" : columnValues[j], mapping ) );
	}

	private void bindHistoryValues(
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
}
