/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.collection.spi.CollectionChangeSet;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

import static org.hibernate.action.queue.decompose.collection.CollectionMutationPlanSupport.applyRemoveRestrictions;
import static org.hibernate.action.queue.decompose.collection.CollectionMutationPlanSupport.applyRowDeleteRestrictions;
import static org.hibernate.action.queue.decompose.collection.CollectionMutationPlanSupport.bindDeleteRestrictions;
import static org.hibernate.action.queue.decompose.collection.CollectionMutationPlanSupport.bindTemporalEndingValue;
import static org.hibernate.action.queue.decompose.collection.CollectionMutationPlanSupport.buildTemporalDeleteMutation;
import static org.hibernate.action.queue.decompose.collection.CollectionMutationPlanSupport.createTableMapping;
import static org.hibernate.action.queue.decompose.collection.CollectionOrdinalSupport.Slot;
import static org.hibernate.action.queue.decompose.collection.CollectionOrdinalSupport.calculateOrdinal;
import static org.hibernate.temporal.TemporalTableStrategy.SINGLE_TABLE;

/**
 * Collection mutation contributor for single-table temporal state management.
 *
 * @author Steve Ebersole
 */
public class TemporalCollectionMutationPlanContributor implements CollectionMutationPlanContributor {
	@Override
	public CollectionJdbcOperations.DeleteRowPlan buildDeleteRowPlan(
			DeleteRowPlanContext context,
			Supplier<CollectionJdbcOperations.DeleteRowPlan> standardPlanSupplier) {
		final TemporalMapping temporalMapping = resolveTemporalMapping( context );
		if ( temporalMapping == null ) {
			return standardPlanSupplier.get();
		}

		final var tableMapping = createTableMapping( context.tableDescriptor() );
		final var mutatingTable = new MutatingTableReference( tableMapping );
		final var updateBuilder = new TableUpdateBuilderStandard<>(
				context.persister(),
				mutatingTable,
				context.factory(),
				context.persister().getSqlWhereString()
		);
		applyRowDeleteRestrictions( context.persister(), null, updateBuilder );
		final var mutation = buildTemporalDeleteMutation( updateBuilder, mutatingTable, temporalMapping );
		return new CollectionJdbcOperations.DeleteRowPlan(
				mutation.createMutationOperation( null, context.factory() ),
				(collection, key, rowValue, rowPosition, session, valueBindings) -> {
					bindTemporalEndingValue( context.persister(), valueBindings, session );
					bindDeleteRestrictions(
							context.persister(),
							collection,
							key,
							rowValue,
							rowPosition,
							session,
							valueBindings
					);
				}
		);
	}

	@Override
	public MutationOperation buildRemoveOperation(
			RemoveOperationContext context,
			Supplier<MutationOperation> standardOperationSupplier) {
		final TemporalMapping temporalMapping = resolveTemporalMapping( context );
		if ( temporalMapping == null ) {
			return standardOperationSupplier.get();
		}

		final var tableMapping = createTableMapping( context.tableDescriptor() );
		final var mutatingTable = new MutatingTableReference( tableMapping );
		final var updateBuilder = new TableUpdateBuilderStandard<>(
				context.persister(),
				mutatingTable,
				context.factory(),
				context.persister().getSqlWhereString()
		);
		applyRemoveRestrictions( context.persister(), null, updateBuilder );
		return buildTemporalDeleteMutation( updateBuilder, mutatingTable, temporalMapping )
				.createMutationOperation( null, context.factory() );
	}

	@Override
	public void bindRemoveValues(
			RemoveBindContext context,
			org.hibernate.action.queue.bind.JdbcValueBindings valueBindings) {
		bindTemporalEndingValue( context.persister(), valueBindings, context.session() );
		CollectionMutationPlanContributor.super.bindRemoveValues( context, valueBindings );
	}

	@Override
	public boolean contributeValueChange(
			ValueChangeContext context,
			Consumer<FlushOperation> operationConsumer) {
		if ( !isSingleTableTemporalCollection( context ) ) {
			return false;
		}
		if ( context.jdbcOperations().deleteRowPlan() == null || context.jdbcOperations().insertRowPlan() == null ) {
			return false;
		}

		final var valueChange = context.valueChange();
		final Object deleteRowValue = new CollectionChangeSet.Removal(
				valueChange.oldValue(),
				valueChange.index()
		);
		operationConsumer.accept( new FlushOperation(
				context.tableDescriptor(),
				MutationKind.DELETE,
				context.jdbcOperations().deleteRowPlan().jdbcOperation(),
				new SingleRowDeleteBindPlan(
						context.persister(),
						context.collection(),
						context.key(),
						deleteRowValue,
						context.jdbcOperations().deleteRowPlan().restrictions()
				),
				calculateOrdinal( context.ordinalBase(), Slot.DELETE ),
				"DeleteValue[" + valueChange.index() + "](" + context.persister().getRolePath() + ")"
		) );

		final boolean isMap = context.persister().getCollectionSemantics().getCollectionClassification().isMap();
		final Object insertRowValue;
		final int entryIndex;
		if ( isMap ) {
			insertRowValue = Map.entry( valueChange.index(), valueChange.newValue() );
			entryIndex = -1;
		}
		else {
			insertRowValue = valueChange.newValue();
			entryIndex = (Integer) valueChange.index();
		}
		operationConsumer.accept( new FlushOperation(
				context.tableDescriptor(),
				MutationKind.INSERT,
				context.jdbcOperations().insertRowPlan().jdbcOperation(),
				new SingleRowInsertBindPlan(
						context.persister(),
						context.jdbcOperations().insertRowPlan().values(),
						context.collection(),
						context.key(),
						insertRowValue,
						entryIndex
				),
				calculateOrdinal( context.ordinalBase(), Slot.INSERT ),
				"InsertValue[" + valueChange.index() + "](" + context.persister().getRolePath() + ")"
		) );
		return true;
	}

	private static TemporalMapping resolveTemporalMapping(DeleteRowPlanContext context) {
		return isSingleTableTemporalCollection( context )
				? context.persister().getAttributeMapping().getTemporalMapping()
				: null;
	}

	private static TemporalMapping resolveTemporalMapping(RemoveOperationContext context) {
		return isSingleTableTemporalCollection( context )
				? context.persister().getAttributeMapping().getTemporalMapping()
				: null;
	}

	private static boolean isSingleTableTemporalCollection(DeleteRowPlanContext context) {
		return context.persister().getAttributeMapping().getTemporalMapping() != null
			&& context.factory().getSessionFactoryOptions().getTemporalTableStrategy() == SINGLE_TABLE;
	}

	private static boolean isSingleTableTemporalCollection(RemoveOperationContext context) {
		return context.persister().getAttributeMapping().getTemporalMapping() != null
			&& context.factory().getSessionFactoryOptions().getTemporalTableStrategy() == SINGLE_TABLE;
	}

	private static boolean isSingleTableTemporalCollection(ValueChangeContext context) {
		return context.persister().getAttributeMapping().getTemporalMapping() != null
			&& context.session().getFactory().getSessionFactoryOptions().getTemporalTableStrategy() == SINGLE_TABLE;
	}
}
