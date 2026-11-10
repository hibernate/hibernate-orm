package org.hibernate.action.queue.internal.decompose.collection;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationPlanContributor;

import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.action.queue.spi.meta.CollectionTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.mutation.TemporalMutationHelper;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping.MutationDetails;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilderStandard;

import java.util.function.Consumer;

import static org.hibernate.action.queue.internal.decompose.collection.CollectionOrdinalSupport.Slot;
import static org.hibernate.action.queue.internal.decompose.collection.CollectionOrdinalSupport.calculateOrdinal;
import static org.hibernate.temporal.TemporalTableStrategy.HISTORY_TABLE;

/// Collection mutation contributor for temporal history-table state management.
///
/// @author Steve Ebersole
public class HistoryCollectionMutationPlanContributor implements CollectionMutationPlanContributor {
	@Override
	public void contributeAdditionalInsert(
			RowInsertContext context,
			Consumer<FlushOperation> operationConsumer) {
		if ( context.tableDescriptor() instanceof CollectionTableDescriptor collectionTableDescriptor ) {
			final var temporalMapping = resolveTemporalMapping( context );
			if ( temporalMapping != null ) {
				final var historyTableDescriptor = createHistoryTableDescriptor(
						collectionTableDescriptor,
						temporalMapping
				);
				final var historyInsert = buildHistoryInsertOperation(
						context.persister(),
						historyTableDescriptor,
						temporalMapping,
						context.factory()
				);

				operationConsumer.accept( new FlushOperation(
						historyTableDescriptor,
						MutationKind.INSERT,
						historyInsert,
						new HistoryInsertBindPlan(
								context.persister(),
								context.collection(),
								context.key(),
								context.rowValue(),
								context.rowPosition(),
								temporalMapping
						),
						calculateOrdinal( context.ordinalBase(), Slot.INSERT ) + 500,
						"InsertRow(" + context.persister().getRole() + "#history)"
				) );
			}
		}
	}

	@Override
	public void contributeCollectionChange(CollectionChangeContext context, Consumer<FlushOperation> operations) {
		final String historyTable = context.persister().getAttributeMapping().getTemporalMapping().getTableName();
		FlushOperation lastEnd = null;
		// End every old interval before any new interval can match an open-end restriction.
		// The explicit prerequisites survive regrouping and do not depend on encounter order.
		for ( var operation : context.plannedOperations() ) {
			if ( operation.getTableExpression().equals( historyTable ) && operation.getKind() == MutationKind.UPDATE ) {
				operation.setExecutionPrerequisite( lastEnd );
				lastEnd = operation;
			}
		}
		if ( lastEnd != null ) {
			for ( var operation : context.plannedOperations() ) {
				if ( operation.getTableExpression().equals( historyTable ) && operation.getKind() == MutationKind.INSERT ) {
					operation.setExecutionPrerequisite( lastEnd );
				}
			}
		}
	}

	@Override
	public void contributeAdditionalDelete(RowDeleteContext context, Consumer<FlushOperation> operations) {
		final var temporal = context.persister().getAttributeMapping().getTemporalMapping();
		final var table = createHistoryTableDescriptor( context.tableDescriptor(), temporal );
		operations.accept( new FlushOperation(
				table, MutationKind.UPDATE,
				buildHistoryEndOperation( context.persister(), table, temporal, context.factory(), context.sqlWhereString(), false ),
				(bindings, operation, session) -> {
					CollectionMutationPlanSupport.bindTemporalEndingValue( context.persister(), bindings, session );
					CollectionMutationPlanSupport.bindDeleteRestrictions( context.persister(), context.collection(),
							context.key(), context.rowValue(), context.rowPosition(), session, bindings );
				},
				calculateOrdinal( context.ordinalBase(), Slot.DELETE ) + 400,
				"EndHistoryRow(" + context.persister().getRole() + ")"
		) );
	}

	@Override
	public void contributeAdditionalRemove(RemoveContext context, Consumer<FlushOperation> operations) {
		final var temporal = context.persister().getAttributeMapping().getTemporalMapping();
		final var table = createHistoryTableDescriptor( context.tableDescriptor(), temporal );
		operations.accept( new FlushOperation(
				table, MutationKind.UPDATE,
				buildHistoryEndOperation( context.persister(), table, temporal, context.factory(), context.sqlWhereString(), true ),
				(bindings, operation, session) -> {
					CollectionMutationPlanSupport.bindTemporalEndingValue( context.persister(), bindings, session );
					CollectionMutationPlanSupport.bindRemoveRestrictions( context.persister(), context.key(), session, bindings );
				},
				calculateOrdinal( context.ordinalBase(), Slot.DELETE ) + 400,
				"EndHistoryCollection(" + context.persister().getRole() + ")"
		) );
	}

	@Override
	public void contributeAdditionalUpdate(RowUpdateContext context, Consumer<FlushOperation> operations) {
		contributeAdditionalDelete( new RowDeleteContext(
				context.persister(), context.tableDescriptor(), context.factory(), context.jdbcOperations(),
				context.collection(), context.key(), context.oldRowValue(), context.oldPosition(),
				context.ordinalBase(), context.sqlWhereString()
		), operations );
		contributeAdditionalInsert( new RowInsertContext(
				context.persister(), context.tableDescriptor(), context.factory(), context.jdbcOperations(),
				context.collection(), context.key(), context.newRowValue(), context.newPosition(), context.ordinalBase()
		), operations );
	}

	private static MutationOperation buildHistoryEndOperation(
			CollectionPersister persister, CollectionTableDescriptor table, TemporalMapping temporal,
			org.hibernate.engine.spi.SessionFactoryImplementor factory, String sqlWhereString, boolean allRows) {
		final var reference = new MutatingTableReference( CollectionMutationPlanSupport.createTableMapping( table ) );
		final var builder = new org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard<>(
				(CollectionMutationTarget) persister, reference, factory, sqlWhereString );
		if ( allRows ) {
			CollectionMutationPlanSupport.applyRemoveRestrictions( persister, null, builder );
		}
		else {
			CollectionMutationPlanSupport.applyRowDeleteRestrictions( persister, null, builder );
		}
		return CollectionMutationPlanSupport.buildTemporalDeleteMutation( builder, reference, temporal )
				.createMutationOperation( null, factory );
	}

	private static MutationOperation buildHistoryInsertOperation(
			CollectionPersister persister,
			CollectionTableDescriptor historyTableDescriptor,
			TemporalMapping temporalMapping,
			org.hibernate.engine.spi.SessionFactoryImplementor factory) {
		final var tableMapping = new TableDescriptorAsTableMapping(
				historyTableDescriptor,
				0,
				false,
				false
		);
		final var insertBuilder = new TableInsertBuilderStandard(
				(CollectionMutationTarget) persister,
				new MutatingTableReference( tableMapping ),
				factory
		);

		final var attributeMapping = persister.getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().forEachInsertable( insertBuilder::addColumnAssignment );

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.forEachInsertable( insertBuilder::addColumnAssignment );
		}
		else if ( indexDescriptor != null ) {
			indexDescriptor.forEachInsertable( insertBuilder::addColumnAssignment );
		}

		attributeMapping.getElementDescriptor().forEachInsertable( insertBuilder::addColumnAssignment );

		final var mutatingTable = insertBuilder.getMutatingTable();
		final var startingColumn = new ColumnReference( mutatingTable, temporalMapping.getStartingColumnMapping() );
		insertBuilder.addColumnAssignment( temporalMapping.createStartingValueBinding( startingColumn ) );
		final var endingColumn = new ColumnReference( mutatingTable, temporalMapping.getEndingColumnMapping() );
		insertBuilder.addColumnAssignment( temporalMapping.createNullEndingValueBinding( endingColumn ) );

		return insertBuilder.buildMutation().createMutationOperation( null, factory );
	}

	private static CollectionTableDescriptor createHistoryTableDescriptor(
			CollectionTableDescriptor tableDescriptor,
			TemporalMapping temporalMapping) {
		return new CollectionTableDescriptor(
				temporalMapping.getTableName(),
				tableDescriptor.navigableRole(),
				tableDescriptor.isJoinTable(),
				tableDescriptor.isInverse(),
				tableDescriptor.isSelfReferential(),
				tableDescriptor.hasUniqueConstraints(),
				tableDescriptor.cascadeDeleteEnabled(),
				new MutationDetails(
						MutationType.INSERT,
						tableDescriptor.insertDetails().getExpectation(),
						null,
						false,
						tableDescriptor.insertDetails().isDynamicMutation()
				),
				tableDescriptor.updateDetails(),
				tableDescriptor.deleteDetails(),
				tableDescriptor.deleteAllDetails(),
				tableDescriptor.keyDescriptor()
		);
	}

	private static TemporalMapping resolveTemporalMapping(RowInsertContext context) {
		return context.persister().getAttributeMapping().getTemporalMapping() != null
				&& context.factory().getSessionFactoryOptions().getTemporalTableStrategy() == HISTORY_TABLE
				? context.persister().getAttributeMapping().getTemporalMapping()
				: null;
	}

	private static class HistoryInsertBindPlan implements BindPlan {
		private final CollectionPersister persister;
		private final PersistentCollection<?> collection;
		private final Object key;
		private final Object entry;
		private final int entryIndex;
		private final TemporalMapping temporalMapping;

		private HistoryInsertBindPlan(
				CollectionPersister persister,
				PersistentCollection<?> collection,
				Object key,
				Object entry,
				int entryIndex,
				TemporalMapping temporalMapping) {
			this.persister = persister;
			this.collection = collection;
			this.key = key;
			this.entry = entry;
			this.entryIndex = entryIndex;
			this.temporalMapping = temporalMapping;
		}

		@Override
		public void bindValues(
				JdbcValueBindings jdbcValueBindings,
				FlushOperation flushOperation,
				SharedSessionContractImplementor session) {
			if ( key == null ) {
				throw new IllegalArgumentException( "null key for collection: " + persister.getNavigableRole().getFullPath() );
			}
			bindRowValues( jdbcValueBindings, session );
			if ( TemporalMutationHelper.isUsingParameters( session ) ) {
				jdbcValueBindings.bindValue(
						session.getCurrentChangesetIdentifier(),
						temporalMapping.getStartingColumnMapping().getSelectionExpression(),
						ParameterUsage.SET
				);
			}
		}

		private void bindRowValues(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
			final var attributeMapping = persister.getAttributeMapping();
			attributeMapping.getKeyDescriptor().getKeyPart().decompose(
					key,
					jdbcValueBindings::bindAssignment,
					session
			);

			final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
			if ( identifierDescriptor != null ) {
				identifierDescriptor.decompose(
						collection.getIdentifier( entry, entryIndex ),
						jdbcValueBindings::bindAssignment,
						session
				);
			}
			else {
				final var indexDescriptor = attributeMapping.getIndexDescriptor();
				if ( indexDescriptor != null ) {
					indexDescriptor.decompose(
							persister.getIndexIncrementer().apply( collection.getIndex( entry, entryIndex, persister ) ),
							(valueIndex, value, jdbcValueMapping) -> {
								if ( persister.getIndexColumnIsSettable()[valueIndex] ) {
									jdbcValueBindings.bindAssignment( valueIndex, value, jdbcValueMapping );
								}
							},
							session
					);
				}
			}

			attributeMapping.getElementDescriptor().decompose(
					collection.getElement( entry ),
					(valueIndex, value, jdbcValueMapping) -> {
						if ( persister.getElementColumnIsSettable()[valueIndex] ) {
							jdbcValueBindings.bindAssignment( valueIndex, value, jdbcValueMapping );
						}
					},
					session
			);
		}
	}
}
