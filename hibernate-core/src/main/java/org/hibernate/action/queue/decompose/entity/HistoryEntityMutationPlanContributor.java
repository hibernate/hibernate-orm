/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.Checkers;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.bind.OperationResultChecker;
import org.hibernate.action.queue.meta.ColumnDescriptor;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.TemporalMutationHelper;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.LogicalTableUpdate;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.sql.model.ast.builder.AssigningTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_INT_ARRAY;

/// Graph mutation plan contributor for temporal history-table entity mutations.
///
/// History-table state management augments the standard current-table mutation
/// plan.  Current table inserts, updates, and deletes are still emitted by the
/// standard decomposers; this contributor appends the matching history-table
/// operations before the decomposer attaches the action's post-execution
/// callback.
///
/// @author Steve Ebersole
public class HistoryEntityMutationPlanContributor implements EntityMutationPlanContributor {
	private final EntityPersister entityPersister;
	private final SessionFactoryImplementor sessionFactory;
	private final TemporalMapping temporalMapping;
	private final EntityTableDescriptor historyTableDescriptor;
	private final TableDescriptorAsTableMapping historyTableMapping;
	private final TableInsert staticHistoryInsert;
	private final MutationOperation staticHistoryEndOperation;

	public HistoryEntityMutationPlanContributor(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		this.entityPersister = entityPersister;
		this.sessionFactory = sessionFactory;
		this.temporalMapping = entityPersister.getTemporalMapping();
		this.historyTableDescriptor = createHistoryTableDescriptor();
		this.historyTableMapping = new TableDescriptorAsTableMapping(
				historyTableDescriptor,
				historyTableDescriptor.relativePosition(),
				true,
				false
		);
		this.staticHistoryInsert = buildHistoryInsert( entityPersister.getPropertyInsertability(), null, null );
		this.staticHistoryEndOperation = buildHistoryEndOperation().createMutationOperation( null, sessionFactory );
	}

	@Override
	public void contributeAdditionalInsert(
			InsertContext context,
			Consumer<FlushOperation> operationConsumer) {
		final boolean[] inclusions = entityPersister.isDynamicInsert()
				? getPropertiesToInsert( context.state() )
				: entityPersister.getPropertyInsertability();
		final TableInsert insert = entityPersister.isDynamicInsert()
				? buildHistoryInsert( inclusions, context.entity(), context.session() )
				: staticHistoryInsert;

		operationConsumer.accept( new FlushOperation(
				historyTableDescriptor,
				MutationKind.INSERT,
				insert.createMutationOperation( null, sessionFactory ),
				new HistoryInsertBindPlan(
						context.entity(),
						context.identifier(),
						context.state(),
						inclusions
				),
				context.ordinalBase() * 1_000 + 500,
				"EntityInsertAction(" + entityPersister.getEntityName() + "#history)"
		) );
	}

	@Override
	public void contributeAdditionalUpdate(
			UpdateContext context,
			Consumer<FlushOperation> operationConsumer) {
		final int[] dirtyFields = context.action().getDirtyFields();
		if ( entityPersister.excludedFromTemporalVersioning( dirtyFields, context.action().hasDirtyCollection() ) ) {
			contributeHistoryExcludedUpdate( context, operationConsumer );
		}
		else {
			operationConsumer.accept( createHistoryEndOperation(
					context.ordinalBase(),
					"EntityUpdateAction(" + entityPersister.getEntityName() + "#history-end)",
					context.identifier(),
					context.rowId(),
					context.previousVersion(),
					context.previousState()
			) );
			operationConsumer.accept( createHistoryInsertOperation(
					context.ordinalBase(),
					501,
					"EntityUpdateAction(" + entityPersister.getEntityName() + "#history-insert)",
					context.entity(),
					context.identifier(),
					context.state(),
					entityPersister.getPropertyInsertability()
			) );
		}
	}

	@Override
	public void contributeAdditionalDelete(
			DeleteContext context,
			Consumer<FlushOperation> operationConsumer) {
		final var entityEntry = context.session().getPersistenceContextInternal().getEntry( context.action().getInstance() );
		final Object[] loadedState = entityEntry == null ? context.state() : entityEntry.getLoadedState();
		final Object rowId = entityEntry == null ? null : entityEntry.getRowId();

		operationConsumer.accept( createHistoryEndOperation(
				context.ordinalBase(),
				"EntityDeleteAction(" + entityPersister.getEntityName() + "#history)",
				context.identifier(),
				rowId,
				context.version(),
				loadedState
		) );
	}

	private void contributeHistoryExcludedUpdate(
			UpdateContext context,
			Consumer<FlushOperation> operationConsumer) {
		final int[] dirtyFields = context.action().getDirtyFields();
		if ( dirtyFields == null || dirtyFields.length == 0 ) {
			return;
		}

		final HistoryExcludedUpdateDetails details = buildHistoryExcludedUpdate(
				context.entity(),
				context.rowId(),
				dirtyFields,
				context.session()
		);
		if ( details == null ) {
			return;
		}

		operationConsumer.accept( new FlushOperation(
				historyTableDescriptor,
				MutationKind.UPDATE,
				details.operation.createMutationOperation( null, sessionFactory ),
				new HistoryExcludedUpdateBindPlan(
						context.entity(),
						context.identifier(),
						context.rowId(),
						context.state(),
						context.previousState() == null ? context.state() : context.previousState(),
						context.previousVersion(),
						details.attributeIndexes,
						details.applyVersionRestriction
				),
				context.ordinalBase() * 1_000 + 500,
				"EntityUpdateAction(" + entityPersister.getEntityName() + "#history-excluded)"
		) );
	}

	private FlushOperation createHistoryEndOperation(
			int ordinalBase,
			String description,
			Object identifier,
			Object rowId,
			Object version,
			Object[] loadedState) {
		return new FlushOperation(
				historyTableDescriptor,
				MutationKind.UPDATE,
				staticHistoryEndOperation,
				new HistoryEndBindPlan(
						identifier,
						rowId,
						version,
						loadedState
				),
				ordinalBase * 1_000 + 500,
				description
		);
	}

	private FlushOperation createHistoryInsertOperation(
			int ordinalBase,
			int localOrdinal,
			String description,
			Object entity,
			Object identifier,
			Object[] state,
			boolean[] inclusions) {
		return new FlushOperation(
				historyTableDescriptor,
				MutationKind.INSERT,
				staticHistoryInsert.createMutationOperation( null, sessionFactory ),
				new HistoryInsertBindPlan(
						entity,
						identifier,
						state,
						inclusions
				),
				ordinalBase * 1_000 + localOrdinal,
				description
		);
	}

	private EntityTableDescriptor createHistoryTableDescriptor() {
		final EntityTableDescriptor identifierTable = entityPersister.getIdentifierTableDescriptor();
		final List<ColumnDescriptor> columns = new ArrayList<>( identifierTable.columns() );
		columns.add( ColumnDescriptor.from( temporalMapping.getStartingColumnMapping() ) );
		columns.add( ColumnDescriptor.from( temporalMapping.getEndingColumnMapping() ) );

		return new EntityTableDescriptor(
				temporalMapping.getTableName(),
				identifierTable.relativePosition(),
				true,
				false,
				false,
				identifierTable.isSelfReferential(),
				false,
				false,
				new TableMapping.MutationDetails(
						MutationType.INSERT,
						identifierTable.insertDetails().getExpectation(),
						null,
						false,
						entityPersister.isDynamicInsert()
				),
				new TableMapping.MutationDetails(
						MutationType.UPDATE,
						identifierTable.updateDetails().getExpectation(),
						null,
						false,
						entityPersister.isDynamicUpdate()
				),
				new TableMapping.MutationDetails(
						MutationType.DELETE,
						identifierTable.deleteDetails().getExpectation(),
						null,
						false
				),
				columns,
				identifierTable.attributes(),
				identifierTable.attributeColumnIndexes(),
				identifierTable.keyDescriptor()
		);
	}

	private TableInsert buildHistoryInsert(
			boolean[] propertyInclusions,
			Object entity,
			SharedSessionContractImplementor session) {
		final var insertBuilder = new TableInsertBuilderStandard(
				entityPersister,
				historyTableMapping,
				sessionFactory
		);
		applyHistoryInsertDetails( insertBuilder, propertyInclusions, entity, session );
		return insertBuilder.buildMutation();
	}

	private void applyHistoryInsertDetails(
			TableInsertBuilderStandard insertBuilder,
			boolean[] propertyInclusions,
			Object entity,
			SharedSessionContractImplementor session) {
		final var attributeMappings = entityPersister.getAttributeMappings();
		for ( final var attribute : historyTableDescriptor.attributes() ) {
			final int attributeIndex = attribute.getStateArrayPosition();
			if ( propertyInclusions[attributeIndex] ) {
				attribute.forEachInsertable( insertBuilder );
			}
			else {
				final var generator = attributeMappings.get( attributeIndex ).getGenerator();
				if ( isValueGeneratedOnInsert( generator ) ) {
					if ( session != null && generator.generatedBeforeExecution( entity, session ) ) {
						propertyInclusions[attributeIndex] = true;
						attribute.forEachInsertable( insertBuilder );
					}
					else if ( isValueGenerationInSql( generator, INSERT ) ) {
						addSqlGeneratedValue( insertBuilder, attribute, (OnExecutionGenerator) generator, INSERT );
					}
				}
			}
		}

		final var mutatingTable = insertBuilder.getMutatingTable();
		final var startingColumn = new ColumnReference( mutatingTable, temporalMapping.getStartingColumnMapping() );
		insertBuilder.addColumnAssignment( temporalMapping.createStartingValueBinding( startingColumn ) );
		final var endingColumn = new ColumnReference( mutatingTable, temporalMapping.getEndingColumnMapping() );
		insertBuilder.addColumnAssignment( temporalMapping.createNullEndingValueBinding( endingColumn ) );
		historyTableDescriptor.keyDescriptor().columns().forEach( insertBuilder::addColumnAssignment );
	}

	private LogicalTableUpdate<?> buildHistoryEndOperation() {
		final var updateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference( historyTableMapping ),
				sessionFactory
		);

		historyTableDescriptor.keyDescriptor().columns().forEach( updateBuilder::addKeyRestriction );
		addTemporalEnding( updateBuilder );
		applyPartitionKeyRestriction( updateBuilder );
		if ( entityPersister.optimisticLockStyle() == OptimisticLockStyle.VERSION
				&& entityPersister.getVersionMapping() != null ) {
			updateBuilder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}

		return updateBuilder.buildMutation();
	}

	private HistoryExcludedUpdateDetails buildHistoryExcludedUpdate(
			Object entity,
			Object rowId,
			int[] dirtyAttributeIndexes,
			SharedSessionContractImplementor session) {
		final boolean[] dirtyFlags = new boolean[entityPersister.getAttributeMappings().size()];
		for ( int dirtyAttributeIndex : dirtyAttributeIndexes ) {
			dirtyFlags[dirtyAttributeIndex] = true;
		}

		final var updateability = entityPersister.hasUninitializedLazyProperties( entity )
				? entityPersister.getNonLazyPropertyUpdateability()
				: entityPersister.getPropertyUpdateability();
		final var updateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference( historyTableMapping ),
				sessionFactory
		);
		final List<Integer> bindableAttributeIndexes = new ArrayList<>();
		boolean hasValues = false;

		for ( final var attribute : historyTableDescriptor.attributes() ) {
			final int attributeIndex = attribute.getStateArrayPosition();
			if ( !entityPersister.isPropertyTemporalExcluded( attributeIndex ) ) {
				continue;
			}

			final Generator generator = attribute.getGenerator();
			final boolean generatedInSql = needsUpdateValueGeneration( entity, session, generator );
			final boolean include = generatedInSql
					|| isGeneratedBeforeExecution( entity, session, generator )
					|| dirtyFlags[attributeIndex] && updateability[attributeIndex];

			if ( include ) {
				if ( generatedInSql ) {
					final var onExecutionGenerator = (OnExecutionGenerator) generator;
					addSqlGeneratedValue( updateBuilder, attribute, onExecutionGenerator, UPDATE );
					hasValues = true;
					if ( onExecutionGenerator.writePropertyValue( UPDATE ) ) {
						bindableAttributeIndexes.add( attributeIndex );
					}
				}
				else {
					attribute.forEachUpdatable( updateBuilder );
					hasValues = true;
					bindableAttributeIndexes.add( attributeIndex );
				}
			}
		}

		if ( !hasValues ) {
			return null;
		}

		if ( rowId != null && entityPersister.getRowIdMapping() != null ) {
			updateBuilder.addKeyRestrictionLeniently( entityPersister.getRowIdMapping() );
		}
		else {
			historyTableDescriptor.keyDescriptor().columns().forEach( updateBuilder::addKeyRestriction );
		}
		addCurrentRowRestriction( updateBuilder );
		applyPartitionKeyRestriction( updateBuilder );
		if ( entityPersister.optimisticLockStyle() == OptimisticLockStyle.VERSION
				&& entityPersister.getVersionMapping() != null ) {
			updateBuilder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}

		return new HistoryExcludedUpdateDetails(
				updateBuilder.buildMutation(),
				toIntArray( bindableAttributeIndexes ),
				entityPersister.optimisticLockStyle() == OptimisticLockStyle.VERSION
						&& entityPersister.getVersionMapping() != null
		);
	}

	private void addTemporalEnding(TableUpdateBuilderStandard<MutationOperation> updateBuilder) {
		final var endingColumn = new ColumnReference(
				updateBuilder.getMutatingTable(),
				temporalMapping.getEndingColumnMapping()
		);
		updateBuilder.addColumnAssignment( temporalMapping.createEndingValueBinding( endingColumn ) );
		updateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumn ) );
	}

	private void addCurrentRowRestriction(TableUpdateBuilderStandard<MutationOperation> updateBuilder) {
		final var endingColumn = new ColumnReference(
				updateBuilder.getMutatingTable(),
				temporalMapping.getEndingColumnMapping()
		);
		updateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumn ) );
	}

	private void applyPartitionKeyRestriction(TableUpdateBuilderStandard<MutationOperation> updateBuilder) {
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			final var attributeMappings = entityPersister.getAttributeMappings();
			for ( int m = 0; m < attributeMappings.size(); m++ ) {
				final var attributeMapping = attributeMappings.get( m );
				for ( int i = 0; i < attributeMapping.getJdbcTypeCount(); i++ ) {
					final var selectableMapping = attributeMapping.getSelectable( i );
					if ( selectableMapping.isPartitioned() ) {
						updateBuilder.addKeyRestrictionLeniently( selectableMapping );
					}
				}
			}
		}
	}

	private static boolean[] getPropertiesToInsert(Object[] fields) {
		final boolean[] notNull = new boolean[fields.length];
		for ( int i = 0; i < fields.length; i++ ) {
			notNull[i] = fields[i] != null;
		}
		return notNull;
	}

	private void addSqlGeneratedValue(
			AssigningTableMutationBuilder<?> builder,
			AttributeMapping attributeMapping,
			OnExecutionGenerator generator,
			org.hibernate.generator.EventType eventType) {
		final boolean writePropertyValue = generator.writePropertyValue( eventType );
		final var columnValues = writePropertyValue
				? null
				: generator.getReferencedColumnValues( sessionFactory.getJdbcServices().getDialect(), eventType );
		attributeMapping.forEachSelectable( (j, mapping) ->
				builder.addColumnAssignment( mapping, writePropertyValue ? "?" : columnValues[j] ) );
	}

	private boolean isValueGenerationInSql(Generator generator, org.hibernate.generator.EventType eventType) {
		return ( (OnExecutionGenerator) generator ).referenceColumnsInSql(
				sessionFactory.getJdbcServices().getDialect(),
				eventType
		);
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

	private boolean needsUpdateValueGeneration(
			Object entity,
			SharedSessionContractImplementor session,
			Generator generator) {
		return isValueGeneratedOnUpdate( generator )
			&& ( session == null && generator.generatedOnExecution() || generator.generatedOnExecution( entity, session ) )
			&& isValueGenerationInSql( generator, UPDATE );
	}

	private static boolean isGeneratedBeforeExecution(
			Object entity,
			SharedSessionContractImplementor session,
			Generator generator) {
		return generator != null
			&& generator.generatesOnUpdate()
			&& generator.generatedBeforeExecution( entity, session );
	}

	private static int[] toIntArray(List<Integer> values) {
		if ( values.isEmpty() ) {
			return EMPTY_INT_ARRAY;
		}
		final int[] result = new int[values.size()];
		for ( int i = 0; i < values.size(); i++ ) {
			result[i] = values.get( i );
		}
		return result;
	}

	private final class HistoryInsertBindPlan implements BindPlan, OperationResultChecker {
		private final Object entity;
		private final Object identifier;
		private final Object[] state;
		private final boolean[] inclusions;

		private HistoryInsertBindPlan(
				Object entity,
				Object identifier,
				Object[] state,
				boolean[] inclusions) {
			this.entity = entity;
			this.identifier = identifier;
			this.state = state;
			this.inclusions = inclusions;
		}

		@Override
		public Object getEntityId() {
			return identifier;
		}

		@Override
		public Object getEntityInstance() {
			return entity;
		}

		@Override
		public void bindValues(
				JdbcValueBindings valueBindings,
				FlushOperation flushOperation,
				SharedSessionContractImplementor session) {
			bindKey( identifier, valueBindings, session, ParameterUsage.SET );
			for ( final var attribute : historyTableDescriptor.attributes() ) {
				final int attributeIndex = attribute.getStateArrayPosition();
				if ( inclusions[attributeIndex] && !( attribute instanceof PluralAttributeMapping ) ) {
					attribute.decompose(
							state[attributeIndex],
							0,
							valueBindings,
							null,
							(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
								if ( selectableMapping.isInsertable() && !selectableMapping.isFormula() ) {
									bindings.bindValue(
											jdbcValue,
											selectableMapping.getSelectionExpression(),
											ParameterUsage.SET
									);
								}
							},
							session
					);
				}
			}
			bindTemporalStartingValue( valueBindings, session );
		}

		@Override
		public OperationResultChecker getOperationResultChecker() {
			return this;
		}

		@Override
		public boolean checkResult(
				int affectedRowCount,
				int batchPosition,
				String sqlString,
				SessionFactoryImplementor sessionFactory) throws SQLException {
			return Checkers.identifiedResultsCheck(
					historyTableDescriptor.insertDetails().getExpectation(),
					affectedRowCount,
					batchPosition,
					entityPersister,
					historyTableDescriptor,
					identifier,
					sqlString,
					sessionFactory
			);
		}
	}

	private final class HistoryEndBindPlan implements BindPlan, OperationResultChecker {
		private final Object identifier;
		private final Object rowId;
		private final Object version;
		private final Object[] loadedState;

		private HistoryEndBindPlan(
				Object identifier,
				Object rowId,
				Object version,
				Object[] loadedState) {
			this.identifier = identifier;
			this.rowId = rowId;
			this.version = version;
			this.loadedState = loadedState;
		}

		@Override
		public Object getEntityId() {
			return identifier;
		}

		@Override
		public Object[] getLoadedState() {
			return loadedState;
		}

		@Override
		public void bindValues(
				JdbcValueBindings valueBindings,
				FlushOperation flushOperation,
				SharedSessionContractImplementor session) {
			bindTemporalEndingValue( valueBindings, session );
			if ( rowId != null && entityPersister.getRowIdMapping() != null ) {
				valueBindings.bindValue(
						rowId,
						entityPersister.getRowIdMapping().getRowIdName(),
						ParameterUsage.RESTRICT
				);
			}
			else {
				bindKey( identifier, valueBindings, session, ParameterUsage.RESTRICT );
			}
			bindVersionRestriction( version, valueBindings, session );
			bindPartitionRestrictions( loadedState, valueBindings, session );
		}

		@Override
		public OperationResultChecker getOperationResultChecker() {
			return this;
		}

		@Override
		public boolean checkResult(
				int affectedRowCount,
				int batchPosition,
				String sqlString,
				SessionFactoryImplementor sessionFactory) throws SQLException {
			return Checkers.identifiedResultsCheck(
					historyTableDescriptor.updateDetails().getExpectation(),
					affectedRowCount,
					batchPosition,
					entityPersister,
					historyTableDescriptor,
					identifier,
					sqlString,
					sessionFactory
			);
		}
	}

	private final class HistoryExcludedUpdateBindPlan implements BindPlan, OperationResultChecker {
		private final Object entity;
		private final Object identifier;
		private final Object rowId;
		private final Object[] state;
		private final Object[] loadedState;
		private final Object version;
		private final int[] attributeIndexes;
		private final boolean applyVersionRestriction;

		private HistoryExcludedUpdateBindPlan(
				Object entity,
				Object identifier,
				Object rowId,
				Object[] state,
				Object[] loadedState,
				Object version,
				int[] attributeIndexes,
				boolean applyVersionRestriction) {
			this.entity = entity;
			this.identifier = identifier;
			this.rowId = rowId;
			this.state = state;
			this.loadedState = loadedState;
			this.version = version;
			this.attributeIndexes = attributeIndexes;
			this.applyVersionRestriction = applyVersionRestriction;
		}

		@Override
		public Object getEntityId() {
			return identifier;
		}

		@Override
		public Object getEntityInstance() {
			return entity;
		}

		@Override
		public Object[] getLoadedState() {
			return loadedState;
		}

		@Override
		public void bindValues(
				JdbcValueBindings valueBindings,
				FlushOperation flushOperation,
				SharedSessionContractImplementor session) {
			if ( rowId != null && entityPersister.getRowIdMapping() != null ) {
				valueBindings.bindValue(
						rowId,
						entityPersister.getRowIdMapping().getRowIdName(),
						ParameterUsage.RESTRICT
				);
			}
			else {
				bindKey( identifier, valueBindings, session, ParameterUsage.RESTRICT );
			}
			if ( applyVersionRestriction ) {
				bindVersionRestriction( version, valueBindings, session );
			}
			bindPartitionRestrictions( loadedState, valueBindings, session );
			for ( int attributeIndex : attributeIndexes ) {
				final var attribute = entityPersister.getAttributeMapping( attributeIndex );
				if ( !( attribute instanceof PluralAttributeMapping ) ) {
					attribute.decompose(
							state[attributeIndex],
							0,
							valueBindings,
							null,
							(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
								if ( selectableMapping.isUpdateable() && !selectableMapping.isFormula() ) {
									bindings.bindValue(
											jdbcValue,
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

		@Override
		public OperationResultChecker getOperationResultChecker() {
			return this;
		}

		@Override
		public boolean checkResult(
				int affectedRowCount,
				int batchPosition,
				String sqlString,
				SessionFactoryImplementor sessionFactory) throws SQLException {
			return Checkers.identifiedResultsCheck(
					historyTableDescriptor.updateDetails().getExpectation(),
					affectedRowCount,
					batchPosition,
					entityPersister,
					historyTableDescriptor,
					identifier,
					sqlString,
					sessionFactory
			);
		}
	}

	private void bindKey(
			Object identifier,
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session,
			ParameterUsage usage) {
		entityPersister.getIdentifierMapping().breakDownJdbcValues(
				identifier,
				(index, jdbcValue, jdbcValueMapping) -> {
					final var keyColumn = historyTableDescriptor.keyDescriptor().getSelectable( index );
					valueBindings.bindValue( jdbcValue, keyColumn.getSelectionExpression(), usage );
				},
				session
		);
	}

	private void bindTemporalStartingValue(
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		if ( TemporalMutationHelper.isUsingParameters( session ) ) {
			valueBindings.bindValue(
					session.getCurrentTransactionIdentifier(),
					temporalMapping.getStartingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	private void bindTemporalEndingValue(
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		if ( TemporalMutationHelper.isUsingParameters( session ) ) {
			valueBindings.bindValue(
					session.getCurrentTransactionIdentifier(),
					temporalMapping.getEndingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	private void bindVersionRestriction(
			Object version,
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping != null && entityPersister.optimisticLockStyle() == OptimisticLockStyle.VERSION ) {
			valueBindings.bindValue(
					version,
					versionMapping.getSelectionExpression(),
					ParameterUsage.RESTRICT
			);
		}
	}

	private void bindPartitionRestrictions(
			Object[] loadedState,
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		if ( loadedState == null || !entityPersister.hasPartitionedSelectionMapping() ) {
			return;
		}

		for ( var attribute : historyTableDescriptor.attributes() ) {
			attribute.forEachSelectable( (selectionIndex, selectableMapping) -> {
				if ( selectableMapping.isPartitioned() ) {
					final Object value = loadedState[attribute.getStateArrayPosition()];
					if ( value != null ) {
						attribute.decompose(
								value,
								0,
								valueBindings,
								null,
								(valueIndex, bindings, noop, jdbcValue, selectable) -> {
									if ( selectable.isPartitioned() ) {
										bindings.bindValue(
												jdbcValue,
												selectable.getSelectionExpression(),
												ParameterUsage.RESTRICT
										);
									}
								},
								session
						);
					}
				}
			} );
		}
	}

	private record HistoryExcludedUpdateDetails(
			LogicalTableUpdate<?> operation,
			int[] attributeIndexes,
			boolean applyVersionRestriction) {
	}
}
