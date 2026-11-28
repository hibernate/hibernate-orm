package org.hibernate.action.queue.internal.decompose.entity;

import java.util.function.Consumer;

import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.HistoryRevisionWriter;
import org.hibernate.persister.entity.mutation.PartitionKeyValue;

import static org.hibernate.action.queue.internal.decompose.entity.DecompositionHelper.createChangeRecorder;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/// Graph mutation plan contributor for temporal history-table entity mutations.
///
/// Revisions are copied from the current row of an entity, so this contributor
/// adds no history SQL to the entity mutation plan. It appends a no-op callback
/// carrier after the current-table operations. The callback records the affected
/// entity in the [org.hibernate.temporal.spi.HistoryWorkQueue], which copies the
/// current row to the history table once the row has its final state for the
/// changeset.
///
/// @author Steve Ebersole
public class HistoryEntityMutationPlanContributor implements EntityMutationPlanContributor {
	private final EntityPersister entityPersister;

	public HistoryEntityMutationPlanContributor(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		this.entityPersister = entityPersister;
	}

	private HistoryRevisionWriter revisionWriter() {
		return castNonNull( entityPersister.getHistoryRevisionWriter() );
	}

	@Override
	public void contributeAdditionalInsert(
			InsertContext context,
			Consumer<FlushOperation> operationConsumer) {
		operationConsumer.accept( createChangeRecorder(
				entityPersister, context.ordinalBase(), context.identifier(), context.entity(),
				(id, session) -> session.getHistoryWorkQueue().entityInserted( id, revisionWriter(), session )
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
		final var action = context.action();
		final int[] dirtyFields = action.getDirtyFields();
		final boolean newRevision =
				!entityPersister.excludedFromTemporalVersioning( dirtyFields, action.hasDirtyCollection() );
		// the open revision has the partition key of the row before the update
		final var partitionKeys = PartitionKeyValue.fromState( entityPersister,
				context.previousState() == null ? context.state() : context.previousState(), context.session() );
		operationConsumer.accept( createChangeRecorder(
				entityPersister, context.ordinalBase(), context.identifier(), context.entity(),
				(id, session) -> session.getHistoryWorkQueue()
						.entityUpdated( id, newRevision, dirtyFields, partitionKeys, revisionWriter(), session )
		) );
	}

	@Override
	public void contributeAdditionalDelete(
			DeleteContext context,
			Consumer<FlushOperation> operationConsumer) {
		// the partition key is unknown when there is no loaded state, as for a read-only entity
		final var entityEntry =
				context.session().getPersistenceContextInternal().getEntry( context.action().getInstance() );
		final Object[] loadedState = entityEntry == null ? context.state() : entityEntry.getLoadedState();
		final var partitionKeys = PartitionKeyValue.fromState( entityPersister, loadedState, context.session() );
		operationConsumer.accept( createChangeRecorder(
				entityPersister, context.ordinalBase(), context.identifier(), context.action().getInstance(),
				(id, session) -> session.getHistoryWorkQueue()
						.entityDeleted( id, partitionKeys, revisionWriter(), session )
		) );
	}
}
