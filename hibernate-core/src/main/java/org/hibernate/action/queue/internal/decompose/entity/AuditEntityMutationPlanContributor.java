package org.hibernate.action.queue.internal.decompose.entity;

import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.action.queue.internal.GraphBasedActionQueue;
import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.audit.ModificationType;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityAuditSupport;

import static org.hibernate.action.queue.internal.decompose.entity.DecompositionHelper.createChangeRecorder;

/// Graph mutation plan contributor for audited entity mutations.
///
/// Audit rows are copied from rows which the database holds, so this
/// contributor adds no audit SQL to the entity mutation plan. Instead, it
/// appends a no-op callback carrier after the current-table operations. The
/// callback records the logical audited change in the graph audit collector,
/// where repeated entity changes are merged before the audit rows are written
/// at transaction completion.
///
/// @author Steve Ebersole
public class AuditEntityMutationPlanContributor implements EntityMutationPlanContributor {
	private final EntityPersister entityPersister;
	private final EntityAuditSupport entityAuditSupport;

	public AuditEntityMutationPlanContributor(EntityPersister entityPersister) {
		this.entityPersister = entityPersister;
		this.entityAuditSupport = entityPersister.getEntityAuditSupport();
	}

	@Override
	public void contributeAdditionalInsert(
			InsertContext context,
			Consumer<FlushOperation> operationConsumer) {
		operationConsumer.accept( createChangeRecorder(
				entityPersister, context.ordinalBase(), context.identifier(), context.entity(),
				(id, session) -> recordChange( id, ModificationType.ADD, session )
		) );
	}

	@Override
	public void contributeAdditionalUpdate(
			UpdateContext context,
			Consumer<FlushOperation> operationConsumer) {
		final var action = context.action();
		if ( entityAuditSupport.isAuditedUpdate( action.getDirtyFields(), action.hasDirtyCollection() ) ) {
			operationConsumer.accept( createChangeRecorder(
					entityPersister, context.ordinalBase(), context.identifier(), context.entity(),
					(id, session) -> recordChange( id, ModificationType.MOD, session )
			) );
		}
	}

	@Override
	public void contributeAdditionalDelete(
			DeleteContext context,
			Consumer<FlushOperation> operationConsumer) {
		operationConsumer.accept( createChangeRecorder(
				entityPersister, context.ordinalBase(), context.identifier(), context.action().getInstance(),
				(id, session) -> recordChange( id, ModificationType.DEL, session )
		) );
	}

	private void recordChange(Object identifier, ModificationType modificationType, SessionImplementor session) {
		resolveCollector( session ).getAuditMutationCollector().entityChanged(
				session.generateEntityKey( identifier, entityPersister ),
				modificationType,
				entityAuditSupport
		);
	}

	private GraphBasedActionQueue resolveCollector(SessionImplementor session) {
		final var actionQueue = session.getActionQueue();
		if ( actionQueue instanceof GraphBasedActionQueue graphBasedActionQueue ) {
			return graphBasedActionQueue;
		}
		throw new HibernateException( "Audit graph mutation plan used with non-graph action queue" );
	}
}
