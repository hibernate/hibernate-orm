/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit.spi;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.SharedSessionContract;
import org.hibernate.Session;
import org.hibernate.audit.AuditException;
import org.hibernate.annotations.RevisionEntity;
import org.hibernate.audit.RevisionListener;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.temporal.spi.TransactionIdentifierService;
import org.hibernate.temporal.spi.TransactionIdentifierSupplier;


/**
 * A built-in {@link TransactionIdentifierSupplier} that persists
 * a user-defined revision entity and returns the
 * {@link RevisionEntity.TransactionId @RevisionEntity.TransactionId}
 * property value as the transaction id for audit rows.
 * <p>
 * An optional {@link RevisionListener} callback can be
 * configured for populating custom fields.
 *
 * @param <T> the type of the transaction identifier
 * (the {@link RevisionEntity.TransactionId @TransactionId}
 * property type)
 *
 * @author Marco Belladelli
 * @since 7.4
 */
public class RevisionEntitySupplier<T> implements TransactionIdentifierSupplier<T> {
	private final Class<?> revisionEntityClass;
	private final String transactionIdProperty;
	private final String timestampProperty;
	private final @Nullable String modifiedEntitiesProperty;
	private final @Nullable RevisionListener listener;

	/**
	 * @param revisionEntityClass the revision entity class
	 * @param transactionIdProperty the name of the {@link RevisionEntity.TransactionId @TransactionId} property
	 * @param timestampProperty the name of the {@link RevisionEntity.Timestamp @Timestamp} property
	 * @param modifiedEntitiesProperty the name of the {@link RevisionEntity.ModifiedEntities @ModifiedEntities}
	 * property, or {@code null} if entity change tracking is not configured
	 * @param listener optional callback for populating custom fields
	 */
	public RevisionEntitySupplier(
			Class<?> revisionEntityClass,
			String transactionIdProperty,
			String timestampProperty,
			@Nullable String modifiedEntitiesProperty, @Nullable RevisionListener listener) {
		this.revisionEntityClass = revisionEntityClass;
		this.transactionIdProperty = transactionIdProperty;
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
	 * The name of the {@link RevisionEntity.TransactionId @TransactionId} property.
	 */
	public String getTransactionIdProperty() {
		return transactionIdProperty;
	}

	/**
	 * The name of the {@link RevisionEntity.Timestamp @Timestamp} property.
	 */
	public String getTimestampProperty() {
		return timestampProperty;
	}

	/**
	 * The configured revision listener, or {@code null}.
	 */
	public @Nullable RevisionListener getListener() {
		return listener;
	}

	/**
	 * The name of the {@link RevisionEntity.ModifiedEntities @ModifiedEntities}
	 * property, or {@code null} if entity change tracking is not configured.
	 */
	public @Nullable String getModifiedEntitiesProperty() {
		return modifiedEntitiesProperty;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T generateTransactionIdentifier(SharedSessionContract session) {
		final var sessionImpl = (SharedSessionContractImplementor) session;
		final EntityPersister persister = sessionImpl.getEntityPersister( revisionEntityClass.getName(), null );
		final Object revisionEntity = persister.instantiate( null, sessionImpl );
		if ( listener != null ) {
			listener.newRevision( revisionEntity );
		}
		final var childSession = persistRevisionEntity( session, revisionEntity );
		sessionImpl.getAuditWorkQueue().setRevisionContext( revisionEntity, childSession );
		return (T) readTransactionId( revisionEntity, persister, sessionImpl );
	}

	/**
	 * Read the {@link RevisionEntity.TransactionId @TransactionId} property value
	 * from the revision entity after persistence.
	 * <p>
	 * Handles both regular properties and {@code @Id} properties.
	 */
	private Object readTransactionId(
			Object revisionEntity,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		final Object txId;
		final var txIdAttr = persister.findAttributeMapping( transactionIdProperty );
		if ( txIdAttr != null ) {
			txId = persister.getValue( revisionEntity, txIdAttr.getStateArrayPosition() );
		}
		else {
			// @TransactionId is the @Id
			txId = persister.getIdentifier( revisionEntity, session );
		}
		if ( txId == null ) {
			throw new AuditException(
					"@RevisionEntity.TransactionId property '" + transactionIdProperty
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
	 * Resolve the {@link RevisionEntitySupplier} from the given
	 * service registry, or return {@code null} if no
	 * {@code @RevisionEntity} is configured.
	 */
	public static @Nullable RevisionEntitySupplier<?> resolve(ServiceRegistry registry) {
		final var service = registry.getService( TransactionIdentifierService.class );
		return service != null && service.getIdentifierSupplier() instanceof RevisionEntitySupplier<?> supplier
				? supplier
				: null;
	}
}
