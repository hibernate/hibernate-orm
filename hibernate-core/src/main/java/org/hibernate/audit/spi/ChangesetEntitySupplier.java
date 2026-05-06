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
 * a user-defined changeset entity and returns the
 * {@link ChangesetEntity.ChangesetId @ChangesetEntity.ChangesetId}
 * property value as the changeset id for audit rows.
 * <p>
 * An optional {@link ChangesetListener} callback can be
 * configured for populating custom fields.
 *
 * @param <T> the type of the changeset identifier
 * (the {@link ChangesetEntity.ChangesetId @ChangesetId}
 * property type)
 *
 * @author Marco Belladelli
 * @since 7.4
 */
public class ChangesetEntitySupplier<T> implements ChangesetIdentifierSupplier<T> {
	private final Class<?> changesetEntityClass;
	private final String changesetIdProperty;
	private final String timestampProperty;
	private final @Nullable String modifiedEntitiesProperty;
	private final @Nullable ChangesetListener listener;

	/**
	 * @param changesetEntityClass the changeset entity class
	 * @param changesetIdProperty the name of the {@link ChangesetEntity.ChangesetId @ChangesetId} property
	 * @param timestampProperty the name of the {@link ChangesetEntity.Timestamp @Timestamp} property
	 * @param modifiedEntitiesProperty the name of the {@link ChangesetEntity.ModifiedEntities @ModifiedEntities}
	 * property, or {@code null} if entity change tracking is not configured
	 * @param listener optional callback for populating custom fields
	 */
	public ChangesetEntitySupplier(
			Class<?> changesetEntityClass,
			String changesetIdProperty,
			String timestampProperty,
			@Nullable String modifiedEntitiesProperty, @Nullable ChangesetListener listener) {
		this.changesetEntityClass = changesetEntityClass;
		this.changesetIdProperty = changesetIdProperty;
		this.timestampProperty = timestampProperty;
		this.modifiedEntitiesProperty = modifiedEntitiesProperty;
		this.listener = listener;
	}

	/**
	 * The changeset entity class.
	 */
	public Class<?> getChangesetEntityClass() {
		return changesetEntityClass;
	}

	/**
	 * The name of the {@link ChangesetEntity.ChangesetId @Changeset} property.
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
	 * The configured changeset listener, or {@code null}.
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
		final EntityPersister persister = sessionImpl.getEntityPersister( changesetEntityClass.getName(), null );
		final Object changesetEntity = persister.instantiate( null, sessionImpl );
		if ( listener != null ) {
			listener.newChangeset( changesetEntity );
		}
		final var childSession = persistChangesetEntity( session, changesetEntity );
		sessionImpl.getAuditWorkQueue().setChangesetContext( changesetEntity, childSession );
		return (T) readChangesetId( changesetEntity, persister, sessionImpl );
	}

	/**
	 * Read the {@link ChangesetEntity.ChangesetId @Changeset} property value
	 * from the changeset entity after persistence.
	 * <p>
	 * Handles both regular properties and {@code @Id} properties.
	 */
	private Object readChangesetId(
			Object changesetEntity,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		final Object changesetId;
		final var changesetIdAttr = persister.findAttributeMapping( changesetIdProperty );
		if ( changesetIdAttr != null ) {
			changesetId = persister.getValue( changesetEntity, changesetIdAttr.getStateArrayPosition() );
		}
		else {
			// @ChangesetId is the @Id
			changesetId = persister.getIdentifier( changesetEntity, session );
		}
		if ( changesetId == null ) {
			throw new AuditException(
					"@ChangesetEntity.ChangesetId property '" + changesetIdProperty
					+ "' is null after persisting changeset entity '"
					+ changesetEntityClass.getName() + "'"
			);
		}
		return changesetId;
	}

	/**
	 * Persist the changeset entity using a child {@link Session}
	 * that shares the parent session's JDBC connection.
	 * The child session is returned so it can be kept open
	 * for deferred flush of {@code @ElementCollection} changes.
	 */
	private static Session persistChangesetEntity(
			SharedSessionContract session,
			Object changesetEntity) {
		final var childSession = session.sessionWithOptions()
				.connection()
				.openSession();
		childSession.persist( changesetEntity );
		childSession.flush();
		return childSession;
	}

	/**
	 * Resolve the {@link ChangesetEntitySupplier} from the given
	 * service registry, or return {@code null} if no
	 * {@code @ChangesetEntity} is configured.
	 */
	public static @Nullable ChangesetEntitySupplier<?> resolve(ServiceRegistry registry) {
		final var service = registry.getService( ChangesetCoordinator.class );
		return service != null && service.getIdentifierSupplier() instanceof ChangesetEntitySupplier<?> supplier
				? supplier
				: null;
	}
}
