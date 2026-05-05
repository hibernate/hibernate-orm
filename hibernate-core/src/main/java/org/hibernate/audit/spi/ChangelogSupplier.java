/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit.spi;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.SharedSessionContract;
import org.hibernate.Session;
import org.hibernate.action.queue.spi.QueueType;
import org.hibernate.annotations.Changelog;
import org.hibernate.audit.AuditException;
import org.hibernate.audit.ChangesetListener;
import org.hibernate.engine.spi.SessionImplementor;
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
	public T generateIdentifier(SharedSessionContract session) {
		final var sessionImpl = (SharedSessionContractImplementor) session;
		final var context = generateContext( session );
		registerLegacyContext( sessionImpl, context );
		return context.changesetId();
	}

	/**
	 * Generate the complete changelog context for the current transaction.
	 * <p>
	 * This persists the configured {@link Changelog} entity, reads its
	 * changeset identifier, and returns both values together with the child
	 * session which owns deferred changelog collection work. Graph queue audit
	 * execution captures this context directly and passes the identifier into
	 * its bind plans, avoiding the legacy action-queue callback path.
	 *
	 * @param session the session requiring a changeset identifier
	 *
	 * @return the generated changeset context
	 */
	@SuppressWarnings("unchecked")
	public ChangesetContext<T> generateContext(SharedSessionContract session) {
		final var sessionImpl = (SharedSessionContractImplementor) session;
		final EntityPersister persister = sessionImpl.getEntityPersister( changelogClass.getName(), null );
		final Object changelog = persister.instantiate( null, sessionImpl );
		if ( listener != null ) {
			listener.newChangeset( changelog );
		}
		final var childSession = persistChangelog( session, changelog );
		return new ChangesetContext<>(
				(T) readChangesetId( changelog, persister, sessionImpl ),
				changelog,
				childSession
		);
	}

	/**
	 * Register the generated context with the legacy audit infrastructure.
	 * <p>
	 * This is intentionally limited to the legacy queue. Graph queue audit
	 * execution obtains the same context from the session and does not rely on
	 * {@link org.hibernate.action.queue.spi.ActionQueue#setAuditChangesetContext(Object, Session)}.
	 *
	 * @param sessionImpl the session which owns the transaction
	 * @param context the generated changeset context
	 */
	public void registerLegacyContext(
			SharedSessionContractImplementor sessionImpl,
			ChangesetContext<?> context) {
		if ( sessionImpl instanceof SessionImplementor sessionImplementor ) {
			if ( sessionImpl.getFactory().getActionQueueFactory().getConfiguredQueueType() == QueueType.LEGACY ) {
				sessionImplementor.getActionQueue().setAuditChangesetContext(
						context.changelog(),
						context.changesetSession()
				);
			}
		}
		else {
			sessionImpl.getAuditWorkQueue().setChangesetContext(
					context.changelog(),
					context.changesetSession()
			);
		}
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

	/**
	 * Complete transaction changeset context produced by a {@link ChangelogSupplier}.
	 *
	 * @param changesetId the identifier stored in audit rows
	 * @param changelog the persisted changelog entity
	 * @param changesetSession the child session used for deferred changelog collection work
	 *
	 * @param <T> the changeset identifier type
	 */
	public record ChangesetContext<T>(
			T changesetId,
			Object changelog,
			Session changesetSession) {
	}
}
