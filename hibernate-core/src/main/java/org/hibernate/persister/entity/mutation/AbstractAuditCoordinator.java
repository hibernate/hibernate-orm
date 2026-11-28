package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.audit.ModificationType;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Base support for the mutation coordinators of audited entities.
 * <p>
 * A coordinator writes the tables holding current data, and records
 * the modification of the entity. The audit log tables are written by
 * the {@link EntityAuditSupport} of the entity.
 */
@Internal
abstract class AbstractAuditCoordinator extends AbstractMutationCoordinator {
	protected final EntityAuditSupport auditSupport;

	protected AbstractAuditCoordinator(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.auditSupport = castNonNull( entityPersister.getEntityAuditSupport() );
	}

	@Nullable
	@Override
	protected BatchKey getBatchKey() {
		return null;
	}

	/**
	 * Record a modification of the entity, to be written to the audit
	 * log when the transaction completes.
	 */
	protected void recordModification(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull ModificationType modificationType,
			@Nonnull SharedSessionContractImplementor session) {
		recordModification( resolveEntityKey( entity, id, session ), modificationType, session );
	}

	protected void recordModification(
			@Nonnull EntityKey entityKey,
			@Nonnull ModificationType modificationType,
			@Nonnull SharedSessionContractImplementor session) {
		if ( session.isEventSource() ) {
			// the action queue of a stateful session decides where its audit entries are collected
			session.asEventSource().getActionQueue().addAuditEntry( entityKey, modificationType );
		}
		else {
			session.getAuditWorkQueue().enqueue( entityKey, modificationType, auditSupport, session );
		}
	}

	@Nonnull
	private EntityKey resolveEntityKey(@Nonnull Object entity, @Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		final var entityEntry = session.getPersistenceContextInternal().getEntry( entity );
		return entityEntry != null
				? entityEntry.getEntityKey()
				: new EntityKey( id, entityPersister() );
	}
}
