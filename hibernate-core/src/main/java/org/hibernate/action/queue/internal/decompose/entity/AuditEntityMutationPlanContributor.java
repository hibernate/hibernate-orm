/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;

import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.action.queue.internal.GraphBasedActionQueue;
import org.hibernate.action.queue.internal.decompose.collection.DecompositionSupport;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.audit.ModificationType;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityAuditSupport;

/// Graph mutation plan contributor for audited entity mutations.
///
/// Audit table writes are transaction-completion work, so this contributor does
/// not add audit SQL directly to the entity mutation plan.  Instead, it appends a
/// no-op callback carrier after the current-table operations.  The callback
/// records the logical audited change in the graph audit collector, where
/// repeated entity changes are merged before audit operations are materialized at
/// transaction completion.
///
/// @author Steve Ebersole
public class AuditEntityMutationPlanContributor implements EntityMutationPlanContributor {
	private final EntityPersister entityPersister;
	private final EntityAuditSupport entityAuditSupport;
	private final boolean[] auditedPropertyMask;

	public AuditEntityMutationPlanContributor(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		this.entityPersister = entityPersister;
		this.entityAuditSupport = new EntityAuditSupport( entityPersister, sessionFactory );
		this.auditedPropertyMask = entityAuditSupport.getAuditedPropertyMask();
	}

	@Override
	public void contributeAdditionalInsert(
			InsertContext context,
			Consumer<FlushOperation> operationConsumer) {
		operationConsumer.accept( createAuditCallbackCarrier(
				context.ordinalBase(),
				resolveEntityKey( context.identifier(), context.session() ),
				context.entity(),
				context.state(),
				ModificationType.ADD
		) );
	}

	@Override
	public void contributeAdditionalUpdate(
			UpdateContext context,
			Consumer<FlushOperation> operationConsumer) {
		if ( !shouldAuditUpdate( context.action().getDirtyFields(), context.action().hasDirtyCollection() ) ) {
			return;
		}

		operationConsumer.accept( createAuditCallbackCarrier(
				context.ordinalBase(),
				resolveEntityKey( context.identifier(), context.session() ),
				context.entity(),
				context.state(),
				ModificationType.MOD
		) );
	}

	@Override
	public void contributeAdditionalDelete(
			DeleteContext context,
			Consumer<FlushOperation> operationConsumer) {
		final var entityEntry = context.session().getPersistenceContextInternal().getEntry( context.action().getInstance() );
		final Object[] deleteState = entityEntry != null && entityEntry.getLoadedState() != null
				? entityEntry.getLoadedState()
				: context.state();

		operationConsumer.accept( createAuditCallbackCarrier(
				context.ordinalBase(),
				resolveEntityKey( context.identifier(), context.session() ),
				context.action().getInstance(),
				deleteState,
				ModificationType.DEL
		) );
	}

	private FlushOperation createAuditCallbackCarrier(
			int ordinalBase,
			EntityKey entityKey,
			Object entity,
			Object[] state,
			ModificationType modificationType) {
		return DecompositionSupport.createNoOpCallbackCarrier(
				entityPersister.getIdentifierTableDescriptor(),
				ordinalBase * 1_000 + 900,
				session -> resolveCollector( session ).getAuditMutationCollector().entityChanged(
						entityKey,
						entity,
						state,
						modificationType,
						entityAuditSupport
				)
		);
	}

	private EntityKey resolveEntityKey(
			Object identifier,
			SharedSessionContractImplementor session) {
		return session.generateEntityKey( identifier, entityPersister );
	}

	private GraphBasedActionQueue resolveCollector(SessionImplementor session) {
		final var actionQueue = session.getActionQueue();
		if ( actionQueue instanceof GraphBasedActionQueue graphBasedActionQueue ) {
			return graphBasedActionQueue;
		}
		throw new HibernateException( "Audit graph mutation plan used with non-graph action queue" );
	}

	private boolean shouldAuditUpdate(int[] dirtyAttributeIndexes, boolean hasDirtyCollection) {
		if ( dirtyAttributeIndexes == null || dirtyAttributeIndexes.length == 0 ) {
			return true;
		}
		if ( hasDirtyCollection ) {
			return true;
		}
		for ( int dirtyIndex : dirtyAttributeIndexes ) {
			if ( auditedPropertyMask[dirtyIndex] ) {
				return true;
			}
		}
		return false;
	}
}
