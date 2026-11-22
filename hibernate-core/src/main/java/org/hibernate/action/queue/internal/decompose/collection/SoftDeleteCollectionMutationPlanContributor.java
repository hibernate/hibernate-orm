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

import static org.hibernate.action.queue.internal.decompose.collection.CollectionMutationPlanSupport.createTableMapping;

/// Marks collection rows as deleted when removing a single row or clearing a collection.
public final class SoftDeleteCollectionMutationPlanContributor implements CollectionMutationPlanContributor {
	private final SoftDeleteMapping mapping;

	public SoftDeleteCollectionMutationPlanContributor(SoftDeleteMapping mapping) {
		this.mapping = mapping;
	}

	@Override
	public boolean supportsInsertRowCompaction() {
		return true;
	}

	@Override
	public DeleteRowPlan buildDeleteRowPlan(DeleteRowPlanContext context, Supplier<DeleteRowPlan> standardPlanSupplier) {
		final var builder = createUpdateBuilder(
				context.persister(), context.tableDescriptor(), context.factory(), context.sqlWhereString() );
		return CollectionMutationPlanSupport.buildDeleteRowPlan( context, builder );
	}

	@Override
	public MutationOperation buildRemoveOperation(
			RemoveOperationContext context, Supplier<MutationOperation> standardOperationSupplier) {
		final var builder = createUpdateBuilder(
				context.persister(), context.tableDescriptor(), context.factory(), context.sqlWhereString() );
		return CollectionMutationPlanSupport.buildRemoveOperation( context, builder );
	}

	private TableUpdateBuilderStandard<?> createUpdateBuilder(
			CollectionPersister persister, TableDescriptor table, SessionFactoryImplementor factory,
			String where) {
		final var reference = new MutatingTableReference( createTableMapping( table ) );
		final var builder = new TableUpdateBuilderStandard<>(
				(CollectionMutationTarget) persister, reference, factory, where );
		final var indicator = new ColumnReference( reference, mapping );
		builder.addColumnAssignment( mapping.createDeletedValueBinding( indicator ) );
		builder.addNonKeyRestriction( mapping.createNonDeletedValueBinding( indicator ) );
		return builder;
	}
}
