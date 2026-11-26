package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.Checkers;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.meta.ColumnDescriptor;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.spi.plan.FlushOperation;
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
import org.hibernate.persister.entity.mutation.PartitionKeyValue;
import org.hibernate.persister.entity.mutation.TemporalMutationHelper;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.model.LogicalTableUpdate;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableInsert;
import org.hibernate.sql.ast.spi.model.builder.AssigningTableMutationBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilderStandard;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;

import static org.hibernate.action.queue.internal.decompose.entity.DecompositionHelper.initializeStateForRevisionInsert;
import static org.hibernate.action.queue.internal.decompose.entity.DecompositionHelper.revisionInsertability;
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
	private final MutationOperation unversionedHistoryEndOperation;
	private final boolean versioned;

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
		this.versioned = entityPersister.optimisticLockStyle() == OptimisticLockStyle.VERSION
				&& entityPersister.getVersionMapping() != null;
		this.staticHistoryEndOperation = buildHistoryEndOperation( versioned, null );
		this.unversionedHistoryEndOperation =
				versioned ? buildHistoryEndOperation( false, null ) : staticHistoryEndOperation;
	}

	@Override
	public void contributeAdditionalInsert(
			InsertContext context,
			Consumer<FlushOperation> operationConsumer) {
		final boolean[] inclusions = entityPersister.isDynamicInsert()
				? getPropertiesToInsert( context.state() )
				: entityPersister.getPropertyInsertability();
		final var insert = entityPersister.isDynamicInsert()
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
		if ( context.isBeingDeleted() ) {
			// Nullifying current-table foreign keys before deletion creates no historical revision.
			return;
		}
		if ( !context.requiresNewTemporalVersion() ) {
			contributeHistoryInPlaceUpdate( context, operationConsumer );
		}
		else {
			final var end = createHistoryEndOperation(
					context.ordinalBase(),
					"EntityUpdateAction(" + entityPersister.getEntityName() + "#history-end)",
					context.identifier(),
					context.previousVersion(),
					context.previousState(),
					context.session()
			);
			initializeStateForRevisionInsert( context, historyTableDescriptor );
			final var insert = createHistoryInsertOperation( context );
			context.recordRevisionAfterInsert( insert );
			insert.addExecutionPrerequisite( end );
			operationConsumer.accept( end );
			operationConsumer.accept( insert );
		}
	}

	@Override
	public void contributeAdditionalDelete(
			DeleteContext context,
			Consumer<FlushOperation> operationConsumer) {
		final var entityEntry = context.session().getPersistenceContextInternal().getEntry( context.action().getInstance() );
		final Object[] loadedState = entityEntry == null ? context.state() : entityEntry.getLoadedState();

		operationConsumer.accept( createHistoryEndOperation(
				context.ordinalBase(),
				"EntityDeleteAction(" + entityPersister.getEntityName() + "#history)",
				context.identifier(),
				context.version(),
				loadedState,
				context.session()
		) );
	}

	private void contributeHistoryInPlaceUpdate(
			UpdateContext context,
			Consumer<FlushOperation> operationConsumer) {
		final int[] dirtyFields = context.action().getDirtyFields();
		final boolean revisionFromCurrentTransaction = context.updatesRevisionFromCurrentTransaction();
		if ( revisionFromCurrentTransaction || dirtyFields != null && dirtyFields.length != 0 ) {
			final Object[] loadedState = context.previousState() == null ? context.state() : context.previousState();
			final var partitionKeys = PartitionKeyValue.fromState( entityPersister, loadedState, context.session() );
			final var details = buildHistoryInPlaceUpdate(
					context.entity(),
					context.state(),
					dirtyFields,
					revisionFromCurrentTransaction,
					partitionKeys,
					context.session()
			);
			if ( details != null ) {
				operationConsumer.accept( new FlushOperation(
						historyTableDescriptor,
						MutationKind.UPDATE,
						details.operation.createMutationOperation( null, sessionFactory ),
						new HistoryInPlaceUpdateBindPlan(
								context.entity(),
								context.identifier(),
								context.state(),
								loadedState,
								partitionKeys,
								context.previousVersion(),
								details.attributeIndexes,
								details.applyVersionRestriction
						),
						context.ordinalBase() * 1_000 + 500,
						"EntityUpdateAction(" + entityPersister.getEntityName() + "#history-update)"
				) );
			}
		}
	}

	private FlushOperation createHistoryEndOperation(
			int ordinalBase,
			String description,
			Object identifier,
			Object version,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		// the partition key is unknown when there is no loaded state, as for a read-only entity
		final var partitionKeys = PartitionKeyValue.fromState( entityPersister, loadedState, session );
		final MutationOperation operation;
		if ( PartitionKeyValue.needsDynamicRestriction( entityPersister, partitionKeys ) ) {
			operation = buildHistoryEndOperation( versioned && version != null, partitionKeys );
		}
		else {
			operation = version == null ? unversionedHistoryEndOperation : staticHistoryEndOperation;
		}
		return new FlushOperation(
				historyTableDescriptor,
				MutationKind.UPDATE,
				operation,
				new HistoryEndBindPlan(
						identifier,
						version,
						loadedState,
						partitionKeys
				),
				ordinalBase * 1_000 + 500,
				description
		);
	}

	private FlushOperation createHistoryInsertOperation(UpdateContext context) {
		final var insertability = entityPersister.getPropertyInsertability();
		final var inclusions = revisionInsertability( context, insertability );
		final var insert = inclusions == insertability ? staticHistoryInsert
				: buildHistoryInsert( inclusions, context.entity(), context.session() );
		return new FlushOperation(
				historyTableDescriptor,
				MutationKind.INSERT,
				insert.createMutationOperation( null, sessionFactory ),
				new HistoryInsertBindPlan( context.entity(), context.identifier(), context.state(), inclusions ),
				context.ordinalBase() * 1_000 + 501,
				"EntityUpdateAction(" + entityPersister.getEntityName() + "#history-insert)"
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
				identifierTable.insertDetails(),
				identifierTable.updateDetails(),
				identifierTable.deleteDetails(),
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

	private MutationOperation buildHistoryEndOperation(
			boolean applyVersionRestriction,
			List<PartitionKeyValue> partitionKeys) {
		final var updateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference( historyTableMapping ),
				sessionFactory
		);

		historyTableDescriptor.keyDescriptor().columns().forEach( updateBuilder::addKeyRestriction );
		addTemporalEnding( updateBuilder );
		applyPartitionKeyRestriction( updateBuilder, partitionKeys );
		if ( applyVersionRestriction ) {
			updateBuilder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}

		return updateBuilder.buildMutation().createMutationOperation( null, sessionFactory );
	}

	private HistoryInPlaceUpdateDetails buildHistoryInPlaceUpdate(
			Object entity,
			Object[] state,
			int[] dirtyAttributeIndexes,
			boolean revisionFromCurrentTransaction,
			List<PartitionKeyValue> partitionKeys,
			SharedSessionContractImplementor session) {
		final boolean[] dirtyFlags = new boolean[entityPersister.getAttributeMappings().size()];
		if ( dirtyAttributeIndexes != null ) {
			for ( int dirtyAttributeIndex : dirtyAttributeIndexes ) {
				dirtyFlags[dirtyAttributeIndex] = true;
			}
		}

		final var versionMapping = entityPersister.getVersionMapping();
		if ( revisionFromCurrentTransaction && versionMapping != null ) {
			dirtyFlags[versionMapping.getVersionAttribute().getStateArrayPosition()] = true;
		}
		final var updateability = entityPersister.getPropertyUpdateability();
		final var updateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference( historyTableMapping ),
				sessionFactory
		);
		final List<Integer> bindableAttributeIndexes = new ArrayList<>();
		boolean hasValues = false;

		for ( final var attribute : historyTableDescriptor.attributes() ) {
			final int attributeIndex = attribute.getStateArrayPosition();
			if ( !revisionFromCurrentTransaction && !entityPersister.isPropertyTemporalExcluded( attributeIndex ) ) {
				continue;
			}

			final Generator generator = attribute.getGenerator();
			final boolean generatedInSql = needsUpdateValueGeneration( entity, session, generator );
			final boolean include = generatedInSql
					|| isGeneratedBeforeExecution( entity, session, generator )
					|| updateability[attributeIndex] && state[attributeIndex] != LazyPropertyInitializer.UNFETCHED_PROPERTY
						&& ( dirtyAttributeIndexes == null || dirtyFlags[attributeIndex] );

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

		historyTableDescriptor.keyDescriptor().columns().forEach( updateBuilder::addKeyRestriction );
		addCurrentRowRestriction( updateBuilder );
		applyPartitionKeyRestriction( updateBuilder, partitionKeys );
		if ( entityPersister.optimisticLockStyle() == OptimisticLockStyle.VERSION
				&& entityPersister.getVersionMapping() != null ) {
			updateBuilder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}

		return new HistoryInPlaceUpdateDetails(
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

	/// @param partitionKeys the partition key of the row, or `null` for a
	///                      statement with a parameter for each column
	private void applyPartitionKeyRestriction(
			TableUpdateBuilderStandard<MutationOperation> updateBuilder,
			List<PartitionKeyValue> partitionKeys) {
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			// the history table has the partition key columns of the identifier table
			final String identifierTable = entityPersister.getIdentifierTableDescriptor().name();
			PartitionKeyValue.addRestrictions( entityPersister,
					table -> identifierTable.equals( table ) ? updateBuilder : null,
					partitionKeys, TableMapping::getUpdateDetails );
		}
	}

	private void bindPartitionKeyRestriction(
			List<PartitionKeyValue> partitionKeys,
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation) {
		PartitionKeyValue.bindRestrictions( partitionKeys, entityPersister.getIdentifierTableDescriptor().name(),
				valueBindings, flushOperation.getJdbcOperation() );
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
			// an identifier generated by the insert is assigned to the entity by now
			bindKey(
					identifier == null ? entityPersister.getIdentifier( entity, session ) : identifier,
					valueBindings,
					session,
					ParameterUsage.SET
			);
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
				PreparedStatement statement,
				int batchPosition,
				String sqlString,
				SessionFactoryImplementor sessionFactory) throws SQLException {
			return Checkers.identifiedResultsCheck(
					historyTableDescriptor.insertDetails().getExpectation(),
					affectedRowCount,
					statement,
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
		private final Object version;
		private final Object[] loadedState;
		private final List<PartitionKeyValue> partitionKeys;

		private HistoryEndBindPlan(
				Object identifier,
				Object version,
				Object[] loadedState,
				List<PartitionKeyValue> partitionKeys) {
			this.identifier = identifier;
			this.version = version;
			this.loadedState = loadedState;
			this.partitionKeys = partitionKeys;
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
			bindKey( identifier, valueBindings, session, ParameterUsage.RESTRICT );
			if ( version != null ) {
				bindVersionRestriction( version, valueBindings, session );
			}
			bindPartitionKeyRestriction( partitionKeys, valueBindings, flushOperation );
		}

		@Override
		public OperationResultChecker getOperationResultChecker() {
			return this;
		}

		@Override
		public boolean checkResult(
				int affectedRowCount,
				PreparedStatement statement,
				int batchPosition,
				String sqlString,
				SessionFactoryImplementor sessionFactory) throws SQLException {
			return Checkers.identifiedResultsCheck(
					historyTableDescriptor.updateDetails().getExpectation(),
					affectedRowCount,
					statement,
					batchPosition,
					entityPersister,
					historyTableDescriptor,
					identifier,
					sqlString,
					sessionFactory
			);
		}
	}

	private final class HistoryInPlaceUpdateBindPlan implements BindPlan, OperationResultChecker {
		private final Object entity;
		private final Object identifier;
		private final Object[] state;
		private final Object[] loadedState;
		private final List<PartitionKeyValue> partitionKeys;
		private final Object version;
		private final int[] attributeIndexes;
		private final boolean applyVersionRestriction;

		private HistoryInPlaceUpdateBindPlan(
				Object entity,
				Object identifier,
				Object[] state,
				Object[] loadedState,
				List<PartitionKeyValue> partitionKeys,
				Object version,
				int[] attributeIndexes,
				boolean applyVersionRestriction) {
			this.entity = entity;
			this.identifier = identifier;
			this.state = state;
			this.loadedState = loadedState;
			this.partitionKeys = partitionKeys;
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
			bindKey( identifier, valueBindings, session, ParameterUsage.RESTRICT );
			if ( applyVersionRestriction ) {
				bindVersionRestriction( version, valueBindings, session );
			}
			bindPartitionKeyRestriction( partitionKeys, valueBindings, flushOperation );
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
				PreparedStatement statement,
				int batchPosition,
				String sqlString,
				SessionFactoryImplementor sessionFactory) throws SQLException {
			return Checkers.identifiedResultsCheck(
					historyTableDescriptor.updateDetails().getExpectation(),
					affectedRowCount,
					statement,
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
					session.getCurrentChangesetIdentifier(),
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
					session.getCurrentChangesetIdentifier(),
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

	private record HistoryInPlaceUpdateDetails(
			LogicalTableUpdate<?> operation,
			int[] attributeIndexes,
			boolean applyVersionRestriction) {
	}
}
