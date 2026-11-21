package org.hibernate.action.queue.internal.decompose.collection;

import java.util.function.Supplier;

import org.hibernate.action.queue.spi.decompose.collection.CollectionJdbcOperations.DeleteRowPlan;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationPlanContributor;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.MutationOperation;

import static org.hibernate.action.queue.internal.decompose.collection.CollectionMutationPlanSupport.applyRemoveRestrictions;
import static org.hibernate.action.queue.internal.decompose.collection.CollectionMutationPlanSupport.applyRowDeleteRestrictions;
import static org.hibernate.action.queue.internal.decompose.collection.CollectionMutationPlanSupport.bindDeleteRestrictions;
import static org.hibernate.action.queue.internal.decompose.collection.CollectionMutationPlanSupport.createTableMapping;

/// Marks collection rows as deleted when removing a single row or clearing a collection.
public final class SoftDeleteCollectionMutationPlanContributor implements CollectionMutationPlanContributor {
	private final SoftDeleteMapping mapping;

	public SoftDeleteCollectionMutationPlanContributor(SoftDeleteMapping mapping) {
		this.mapping = mapping;
	}

	@Override
	public DeleteRowPlan buildDeleteRowPlan(DeleteRowPlanContext context, Supplier<DeleteRowPlan> standardPlanSupplier) {
		final var persister = context.persister();
		final var builder = updateBuilder( persister, context.tableDescriptor(), context.factory(), context.sqlWhereString() );
		applyRowDeleteRestrictions( persister, null, builder );
		return new DeleteRowPlan(
				builder.buildMutation().createMutationOperation( null, context.factory() ),
				(collection, key, row, position, session, bindings) ->
						bindDeleteRestrictions( persister, collection, key, row, position, session, bindings )
		);
	}

	@Override
	public MutationOperation buildRemoveOperation(RemoveOperationContext context, Supplier<MutationOperation> standardOperationSupplier) {
		final var builder = updateBuilder( context.persister(), context.tableDescriptor(), context.factory(), context.sqlWhereString() );
		applyRemoveRestrictions( context.persister(), null, builder );
		return builder.buildMutation().createMutationOperation( null, context.factory() );
	}

	private TableUpdateBuilderStandard<?> updateBuilder(
			CollectionPersister persister, TableDescriptor table, SessionFactoryImplementor factory, String sqlWhereString) {
		final var reference = new MutatingTableReference( createTableMapping( table ) );
		final var builder = new TableUpdateBuilderStandard<>( (CollectionMutationTarget) persister, reference, factory, sqlWhereString );
		final var indicator = new ColumnReference( reference, mapping );
		builder.addColumnAssignment( mapping.createDeletedValueBinding( indicator ) );
		builder.addNonKeyRestriction( mapping.createNonDeletedValueBinding( indicator ) );
		return builder;
	}
}
