package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jakarta.annotation.Nullable;

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
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.HistoryTableMutation;
import org.hibernate.persister.entity.mutation.PartitionKeyValue;
import org.hibernate.persister.entity.mutation.TemporalMutationHelper;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.spi.mutation.TableMapping.MutationDetails;
import org.hibernate.sql.ast.spi.model.LogicalTableUpdate;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableInsert;
import org.hibernate.sql.ast.spi.model.TableInsertSelect;
import org.hibernate.sql.ast.spi.model.builder.RestrictedTableMutationBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;

import static org.hibernate.action.queue.internal.decompose.entity.DecompositionHelper.initializeStateForRevisionInsert;
import static org.hibernate.persister.entity.mutation.TemporalMutationHelper.createRevisionEndBuilder;

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
	private final HistoryTableMutation historyMutation;
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
		historyMutation = new HistoryTableMutation( entityPersister, historyTableMapping );
		this.staticHistoryInsert = historyMutation.createInsert( entityPersister.getPropertyInsertability() );
		this.versioned = entityPersister.optimisticLockStyle() == OptimisticLockStyle.VERSION
				&& entityPersister.getVersionMapping() != null;
		this.staticHistoryEndOperation = buildHistoryEndOperation( MutationType.UPDATE, versioned, null );
		this.unversionedHistoryEndOperation =
				versioned ? buildHistoryEndOperation( MutationType.UPDATE, false, null ) : staticHistoryEndOperation;
	}

	@Override
	public void contributeAdditionalInsert(InsertContext context, Consumer<FlushOperation> operations) {
		contributeAdditionalInsert( context, null, operations );
	}

	@Override
	public void contributeAdditionalInsert(
			InsertContext context,
			@Nullable FlushOperation identifierTableMutation,
			Consumer<FlushOperation> operationConsumer) {
		final boolean[] inclusions = entityPersister.isDynamicInsert()
				? getPropertiesToInsert( context.state() )
				: entityPersister.getPropertyInsertability();
		final var insert = entityPersister.isDynamicInsert()
				? historyMutation.createInsert( inclusions )
				: staticHistoryInsert;

		operationConsumer.accept( createHistoryInsertOperation(
				insert, new HistoryInsertBindPlan( context.entity(), context.identifier(), context.state(), inclusions ),
				context.ordinalBase() * 1_000 + 500, "EntityInsertAction(" + entityPersister.getEntityName() + "#history)",
				null, identifierTableMutation ) );
	}

	@Override
	public void contributeAdditionalUpdate(UpdateContext context, Consumer<FlushOperation> operations) {
		contributeAdditionalUpdate( context, null, operations );
	}

	@Override
	public void contributeAdditionalUpdate(
			UpdateContext context,
			@Nullable FlushOperation identifierTableMutation,
			Consumer<FlushOperation> operationConsumer) {
		if ( context.isBeingDeleted() ) {
			// Nullifying current-table foreign keys before deletion creates no historical revision.
			return;
		}
		if ( !context.requiresNewTemporalVersion() ) {
			contributeHistoryInPlaceUpdate( context, identifierTableMutation, operationConsumer );
		}
		else {
			final var end = createHistoryEndOperation(
					MutationType.UPDATE,
					context.ordinalBase(),
					"EntityUpdateAction(" + entityPersister.getEntityName() + "#history-end)",
					context.identifier(),
					context.previousVersion(),
					context.previousState(),
					context.session()
			);
			initializeStateForRevisionInsert( context, historyTableDescriptor, true );
			final var insert = createHistoryInsertOperation(
					staticHistoryInsert, new HistoryInsertBindPlan( context.entity(), context.identifier(), context.state(),
							entityPersister.getPropertyInsertability() ),
					context.ordinalBase() * 1_000 + 501,
					"EntityUpdateAction(" + entityPersister.getEntityName() + "#history-insert)", end, identifierTableMutation );
			context.recordRevisionAfterInsert( insert );
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
				context.removesRevisionFromCurrentTransaction() ? MutationType.DELETE : MutationType.UPDATE,
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
			@Nullable FlushOperation identifierTableMutation,
			Consumer<FlushOperation> operationConsumer) {
		final int[] dirtyFields = context.action().getDirtyFields();
		final boolean revisionFromCurrentTransaction = context.updatesRevisionFromCurrentTransaction();
		if ( revisionFromCurrentTransaction || dirtyFields != null && dirtyFields.length != 0 ) {
			final Object[] loadedState = context.previousState() == null ? context.state() : context.previousState();
			final var partitionKeys = PartitionKeyValue.fromState( entityPersister, loadedState, context.session() );
			final var details = buildHistoryInPlaceUpdate(
					context.state(),
					dirtyFields,
					revisionFromCurrentTransaction,
					partitionKeys
			);
			if ( details != null ) {
				final var update = new FlushOperation(
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
								details.assignments.bindableIndexes(),
								details.applyVersionRestriction
						),
						context.ordinalBase() * 1_000 + 500,
						"EntityUpdateAction(" + entityPersister.getEntityName() + "#history-update)"
				);
				if ( details.assignments.readsLiveTable() ) {
					update.addExecutionPrerequisite( identifierTableMutation );
				}
				operationConsumer.accept( update );
			}
		}
	}

	private FlushOperation createHistoryEndOperation(
			MutationType type,
			int ordinalBase,
			String description,
			Object identifier,
			Object version,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		// the partition key is unknown when there is no loaded state, as for a read-only entity
		final var partitionKeys = PartitionKeyValue.fromState( entityPersister, loadedState, session );
		final MutationOperation operation;
		if ( type == MutationType.DELETE || PartitionKeyValue.needsDynamicRestriction( entityPersister, partitionKeys ) ) {
			operation = buildHistoryEndOperation( type, versioned && version != null, partitionKeys );
		}
		else {
			operation = version == null ? unversionedHistoryEndOperation : staticHistoryEndOperation;
		}
		return new FlushOperation(
				historyTableDescriptor,
				type == MutationType.DELETE ? MutationKind.DELETE : MutationKind.UPDATE,
				operation,
				new HistoryEndBindPlan(
						type,
						identifier,
						version,
						loadedState,
						partitionKeys
				),
				ordinalBase * 1_000 + 500,
				description
		);
	}

	private FlushOperation createHistoryInsertOperation(
			TableInsert insert, HistoryInsertBindPlan bindPlan, int ordinal, String description,
			@Nullable FlushOperation previousRevisionEnd, @Nullable FlushOperation identifierTableMutation) {
		final var operation = new FlushOperation( historyTableDescriptor, MutationKind.INSERT,
				insert.createMutationOperation( null, sessionFactory ), bindPlan, ordinal, description );
		operation.addExecutionPrerequisite( previousRevisionEnd );
		if ( insert instanceof TableInsertSelect ) {
			operation.addExecutionPrerequisite( identifierTableMutation );
		}
		return operation;
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

	private MutationOperation buildHistoryEndOperation(
			MutationType type,
			boolean applyVersionRestriction,
			List<PartitionKeyValue> partitionKeys) {
		final var builder = createRevisionEndBuilder( entityPersister, historyTableMapping, type );
		historyTableDescriptor.keyDescriptor().columns().forEach( builder::addKeyRestriction );
		applyPartitionKeyRestriction( builder, partitionKeys );
		if ( applyVersionRestriction ) {
			builder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}

		return builder.buildMutation().createMutationOperation( null, sessionFactory );
	}

	private HistoryInPlaceUpdateDetails buildHistoryInPlaceUpdate(
			Object[] state,
			int[] dirtyAttributeIndexes,
			boolean revisionFromCurrentTransaction,
			List<PartitionKeyValue> partitionKeys) {
		final var updateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference( historyTableMapping ),
				sessionFactory
		);
		final var assignments = historyMutation.addUpdateAssignments( updateBuilder,
				state, dirtyAttributeIndexes, revisionFromCurrentTransaction );
		if ( assignments == null ) {
			return null;
		}

		historyTableDescriptor.keyDescriptor().columns().forEach( updateBuilder::addKeyRestriction );
		updateBuilder.addNullRestriction( temporalMapping.getEndingColumnMapping() );
		applyPartitionKeyRestriction( updateBuilder, partitionKeys );
		if ( entityPersister.optimisticLockStyle() == OptimisticLockStyle.VERSION
				&& entityPersister.getVersionMapping() != null ) {
			updateBuilder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}

		return new HistoryInPlaceUpdateDetails(
				updateBuilder.buildMutation(),
				assignments,
				entityPersister.optimisticLockStyle() == OptimisticLockStyle.VERSION
						&& entityPersister.getVersionMapping() != null
		);
	}

	/// @param partitionKeys the partition key of the row, or `null` for a
	///                      statement with a parameter for each column
	private void applyPartitionKeyRestriction(
			RestrictedTableMutationBuilder<?, ?> builder,
			List<PartitionKeyValue> partitionKeys) {
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			// the history table has the partition key columns of the identifier table
			final String identifierTable = entityPersister.getIdentifierTableDescriptor().name();
			PartitionKeyValue.addRestrictions( entityPersister,
					table -> identifierTable.equals( table ) ? builder : null,
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
			// Identity inserts assign the identifier before the history operation binds it.
			historyMutation.bindInsert(
					identifier == null ? entityPersister.getIdentifier( entity, session ) : identifier,
					state, inclusions, (value, column) -> valueBindings.bindValue( value, column, ParameterUsage.SET ), session );
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
		private final MutationDetails mutationDetails;
		private final Object identifier;
		private final Object version;
		private final Object[] loadedState;
		private final List<PartitionKeyValue> partitionKeys;

		private HistoryEndBindPlan(
				MutationType mutationType,
				Object identifier,
				Object version,
				Object[] loadedState,
				List<PartitionKeyValue> partitionKeys) {
			this.mutationDetails = mutationType == MutationType.DELETE
					? historyTableDescriptor.deleteDetails()
					: historyTableDescriptor.updateDetails();
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
			if ( mutationDetails.getMutationType() == MutationType.UPDATE ) {
				bindTemporalEndingValue( valueBindings, session );
			}
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
					mutationDetails.getExpectation(),
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
			historyMutation.bindUpdate( state, attributeIndexes,
					(value, column) -> valueBindings.bindValue( value, column, ParameterUsage.SET ), session );
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
			HistoryTableMutation.UpdateAssignments assignments,
			boolean applyVersionRestriction) {
	}
}
