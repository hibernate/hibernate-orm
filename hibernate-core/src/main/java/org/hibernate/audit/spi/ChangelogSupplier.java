/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit.spi;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.SharedSessionContract;
import org.hibernate.Session;
import org.hibernate.annotations.Changelog;
import org.hibernate.audit.AuditException;
import org.hibernate.audit.ChangesetListener;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.temporal.spi.ChangesetCoordinator;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;


/**
 * A built-in {@link ChangesetIdentifierSupplier} that persists
 * a user-defined changelog entity and returns the
 * {@link Changelog.ChangesetId @Changelog.ChangesetId}
 * property value as the changeset id for audit rows.
 * <p>
 * An optional {@link ChangesetListener} callback can be
 * configured for populating custom fields.
 *
 * @param <T> the type of the changeset identifier
 * (the {@link Changelog.ChangesetId @ChangesetId}
 * property type)
 *
 * @author Marco Belladelli
 * @since 7.4
 */
public class ChangelogSupplier<T> implements ChangesetIdentifierSupplier<T> {
	private final Class<?> changelogClass;
	private final String changesetIdProperty;
	private final String timestampProperty;
	private final @Nullable String modifiedEntitiesProperty;
	private final @Nullable ChangesetListener listener;

	/**
	 * @param changelogClass the changelog entity class
	 * @param changesetIdProperty the name of the {@link Changelog.ChangesetId @ChangesetId} property
	 * @param timestampProperty the name of the {@link Changelog.Timestamp @Timestamp} property
	 * @param modifiedEntitiesProperty the name of the {@link Changelog.ModifiedEntities @ModifiedEntities}
	 * property, or {@code null} if entity change tracking is not configured
	 * @param listener optional callback for populating custom fields
	 */
	public ChangelogSupplier(
			Class<?> changelogClass,
			String changesetIdProperty,
			String timestampProperty,
			@Nullable String modifiedEntitiesProperty, @Nullable ChangesetListener listener) {
		this.changelogClass = changelogClass;
		this.changesetIdProperty = changesetIdProperty;
		this.timestampProperty = timestampProperty;
		this.modifiedEntitiesProperty = modifiedEntitiesProperty;
		this.listener = listener;
	}

	/**
	 * The changelog entity class.
	 */
	public Class<?> getChangelogClass() {
		return changelogClass;
	}

	/**
	 * The name of the {@link Changelog.ChangesetId @Changeset} property.
	 */
	public String getChangesetIdProperty() {
		return changesetIdProperty;
	}

	/**
	 * The name of the {@link Changelog.Timestamp @Timestamp} property.
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
	 * The name of the {@link Changelog.ModifiedEntities @ModifiedEntities}
	 * property, or {@code null} if entity change tracking is not configured.
	 */
	public @Nullable String getModifiedEntitiesProperty() {
		return modifiedEntitiesProperty;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T generateIdentifier(SharedSessionContract session) {
		final var sessionImpl = (SharedSessionContractImplementor) session;
		final EntityPersister persister = sessionImpl.getEntityPersister( changelogClass.getName(), null );
		final Object changelog = persister.instantiate( null, sessionImpl );
		if ( listener != null ) {
			listener.newChangeset( changelog );
		}
		final var childSession = persistChangelog( session, changelog );
		sessionImpl.getAuditWorkQueue().setChangesetContext( changelog, childSession );
		return (T) readChangesetId( changelog, persister, sessionImpl );
	}

	/**
	 * Read the {@link Changelog.ChangesetId @Changeset} property value
	 * from the changelog entity after persistence.
	 * <p>
	 * Handles both regular properties and {@code @Id} properties.
	 */
	private Object readChangesetId(
			Object changelog,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		final Object changesetId;
		final var changesetIdAttr = persister.findAttributeMapping( changesetIdProperty );
		if ( changesetIdAttr != null ) {
			changesetId = persister.getValue( changelog, changesetIdAttr.getStateArrayPosition() );
		}
		else {
			// @ChangesetId is the @Id
			changesetId = persister.getIdentifier( changelog, session );
		}
		if ( changesetId == null ) {
			throw new AuditException(
					"@Changelog.ChangesetId property '" + changesetIdProperty
					+ "' is null after persisting changelog entity '"
					+ changelogClass.getName() + "'"
			);
		}
		return changesetId;
	}

	/**
	 * Persist the changelog entity using a child {@link Session}
	 * that shares the parent session's JDBC connection.
	 * The child session is returned so it can be kept open
	 * for deferred flush of {@code @ElementCollection} changes.
	 */
	private static Session persistChangelog(
			SharedSessionContract session,
			Object changelog) {
		final var childSession = session.sessionWithOptions()
				.connection()
				.openSession();
		childSession.persist( changelog );
		childSession.flush();
		return childSession;
	}

	/**
	 * Resolve the {@link ChangelogSupplier} from the given
	 * service registry, or return {@code null} if no
	 * {@code @Changelog} is configured.
	 */
	public static @Nullable ChangelogSupplier<?> resolve(ServiceRegistry registry) {
		final var service = registry.getService( ChangesetCoordinator.class );
		return service != null && service.getIdentifierSupplier() instanceof ChangelogSupplier<?> supplier
				? supplier
				: null;
	}
}
