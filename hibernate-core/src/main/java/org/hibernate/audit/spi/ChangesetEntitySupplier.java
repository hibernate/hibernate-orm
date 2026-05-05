/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit.spi;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.SharedSessionContract;
import org.hibernate.Session;
import org.hibernate.annotations.ChangesetEntity;
import org.hibernate.audit.AuditException;
import org.hibernate.audit.ChangesetListener;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.temporal.spi.ChangesetCoordinator;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;


/**
 * A built-in {@link ChangesetIdentifierSupplier} that persists
 * a user-defined revision entity and returns the
 * {@link ChangesetEntity.ChangesetId @RevisionEntity.TransactionId}
 * property value as the transaction id for audit rows.
 * <p>
 * An optional {@link ChangesetListener} callback can be
 * configured for populating custom fields.
 *
 * @param <T> the type of the transaction identifier
 * (the {@link ChangesetEntity.ChangesetId @TransactionId}
 * property type)
 *
 * @author Marco Belladelli
 * @since 7.4
 */
public class ChangesetEntitySupplier<T> implements ChangesetIdentifierSupplier<T> {
	private final Class<?> revisionEntityClass;
	private final String changesetIdProperty;
	private final String timestampProperty;
	private final @Nullable String modifiedEntitiesProperty;
	private final @Nullable ChangesetListener listener;

	/**
	 * @param revisionEntityClass the revision entity class
	 * @param changesetIdProperty the name of the {@link ChangesetEntity.ChangesetId @TransactionId} property
	 * @param timestampProperty the name of the {@link ChangesetEntity.Timestamp @Timestamp} property
	 * @param modifiedEntitiesProperty the name of the {@link ChangesetEntity.ModifiedEntities @ModifiedEntities}
	 * property, or {@code null} if entity change tracking is not configured
	 * @param listener optional callback for populating custom fields
	 */
	public ChangesetEntitySupplier(
			Class<?> revisionEntityClass,
			String changesetIdProperty,
			String timestampProperty,
			@Nullable String modifiedEntitiesProperty, @Nullable ChangesetListener listener) {
		this.revisionEntityClass = revisionEntityClass;
		this.changesetIdProperty = changesetIdProperty;
		this.timestampProperty = timestampProperty;
		this.modifiedEntitiesProperty = modifiedEntitiesProperty;
		this.listener = listener;
	}

	/**
	 * The revision entity class.
	 */
	public Class<?> getRevisionEntityClass() {
		return revisionEntityClass;
	}

	/**
	 * The name of the {@link ChangesetEntity.ChangesetId @TransactionId} property.
	 */
	public String getChangesetIdProperty() {
		return changesetIdProperty;
	}

	/**
	 * The name of the {@link ChangesetEntity.Timestamp @Timestamp} property.
	 */
	public String getTimestampProperty() {
		return timestampProperty;
	}

	/**
	 * The configured revision listener, or {@code null}.
	 */
	public @Nullable ChangesetListener getListener() {
		return listener;
	}

	/**
	 * The name of the {@link ChangesetEntity.ModifiedEntities @ModifiedEntities}
	 * property, or {@code null} if entity change tracking is not configured.
	 */
	public @Nullable String getModifiedEntitiesProperty() {
		return modifiedEntitiesProperty;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T generateIdentifier(SharedSessionContract session) {
		final var sessionImpl = (SharedSessionContractImplementor) session;
		final EntityPersister persister = sessionImpl.getEntityPersister( revisionEntityClass.getName(), null );
		final Object revisionEntity = persister.instantiate( null, sessionImpl );
		if ( listener != null ) {
			listener.newChangeset( revisionEntity );
		}
		final var childSession = persistRevisionEntity( session, revisionEntity );
		sessionImpl.getAuditWorkQueue().setRevisionContext( revisionEntity, childSession );
		return (T) readTransactionId( revisionEntity, persister, sessionImpl );
	}

	/**
	 * Read the {@link ChangesetEntity.ChangesetId @TransactionId} property value
	 * from the revision entity after persistence.
	 * <p>
	 * Handles both regular properties and {@code @Id} properties.
	 */
	private Object readTransactionId(
			Object revisionEntity,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		final Object txId;
		final var txIdAttr = persister.findAttributeMapping( changesetIdProperty );
		if ( txIdAttr != null ) {
			txId = persister.getValue( revisionEntity, txIdAttr.getStateArrayPosition() );
		}
		else {
			// @TransactionId is the @Id
			txId = persister.getIdentifier( revisionEntity, session );
		}
		if ( txId == null ) {
			throw new AuditException(
					"@RevisionEntity.TransactionId property '" + changesetIdProperty
					+ "' is null after persisting revision entity '"
					+ revisionEntityClass.getName() + "'"
			);
		}
		return txId;
	}

	/**
	 * Persist the revision entity using a child {@link Session}
	 * that shares the parent session's JDBC connection.
	 * The child session is returned so it can be kept open
	 * for deferred flush of {@code @ElementCollection} changes.
	 */
	private static Session persistRevisionEntity(
			SharedSessionContract session,
			Object revisionEntity) {
		final var childSession = session.sessionWithOptions()
				.connection()
				.openSession();
		childSession.persist( revisionEntity );
		childSession.flush();
		return childSession;
	}

	/**
	 * Resolve the {@link ChangesetEntitySupplier} from the given
	 * service registry, or return {@code null} if no
	 * {@code @RevisionEntity} is configured.
	 */
	public static @Nullable ChangesetEntitySupplier<?> resolve(ServiceRegistry registry) {
		final var service = registry.getService( ChangesetCoordinator.class );
		return service != null && service.getIdentifierSupplier() instanceof ChangesetEntitySupplier<?> supplier
				? supplier
				: null;
	}
}
