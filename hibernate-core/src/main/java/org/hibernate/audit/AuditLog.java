/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;

import org.hibernate.annotations.ChangesetEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A service for querying the audit log. Provides access
 * to revision history and modification types for
 * {@linkplain org.hibernate.annotations.Audited audited}
 * entities, complementing the transparent point-in-time
 * reads available via
 * {@link org.hibernate.SessionBuilder#atChangeset(Object)
 * atChangeset()} sessions.
 * <p>
 * Obtain an instance via {@link AuditLogFactory#create}.
 * The instance manages an internal session for audit queries;
 * close it when done to release the session and its JDBC
 * connection.
 *
 * @author Marco Belladelli
 * @see AuditLogFactory
 * @since 7.4
 */
public interface AuditLog extends AutoCloseable {

	/**
	 * Close this audit log and release its internal session.
	 */
	@Override
	void close();

	/**
	 * A special changeset identifier that selects all
	 * historical revisions from an audit log table without
	 * filtering. Pass this magic value to
	 * {@link org.hibernate.SessionBuilder#atChangeset(Object)
	 * atChangeset()} to obtain a session that reads all audit
	 * rows, including deletions.
	 * <p>
	 * Usage:
	 * <pre>
	 * try (var s = sf.withOptions()
	 *         .atChangeset(AuditLog.ALL_CHANGESETS).open()) {
	 *     var history = s.createSelectionQuery(
	 *             "from MyEntity where id = :id",
	 *             MyEntity.class)
	 *         .setParameter("id", entityId)
	 *         .getResultList();
	 * }
	 * </pre>
	 *
	 * @see #getHistory(Class, Object)
	 */
	Object ALL_CHANGESETS = new Object();

	/**
	 * List all changeset identifiers where the given entity
	 * was modified, ordered chronologically.
	 *
	 * @param entityClass the audited entity class
	 * @param id the entity identifier
	 *
	 * @return the list of changeset identifiers
	 */
	List<Object> getChangesets(Class<?> entityClass, Object id);

	/**
	 * Get the {@linkplain ModificationType modification type}
	 * (ADD/MOD/DEL) for an entity at a specific changeset.
	 *
	 * @param entityClass the audited entity class
	 * @param id "at the entity identifier
	 * @param changesetId the changeset identifier
	 *
	 * @return the modification type, or {@code null} if the
	 * entity was not modified in that changeset
	 */
	ModificationType getModificationType(Class<?> entityClass, Object id, Object changesetId);

	/**
	 * Check if an entity type is audited.
	 *
	 * @param entityClass the entity class
	 *
	 * @return {@code true} if the entity is audited
	 */
	boolean isAudited(Class<?> entityClass);

	/**
	 * Find an entity snapshot as of a specific changeset,
	 * that is, immediately after the changeset was applied.
	 * Returns the state at the most recent revision at or
	 * before the given changeset.
	 *
	 * @param entityClass the audited entity class
	 * @param id the entity identifier
	 * @param changesetId the changeset identifier
	 * @param <T> the entity type
	 *
	 * @return the entity state in that changeset, or
	 * {@code null} if the entity did not exist
	 * (e.g. before creation or after deletion)
	 */
	<T> T find(Class<T> entityClass, Object id, Object changesetId);

	/**
	 * Find an entity snapshot as of a specific changeset,
	 * that is, immediately after the changeset was applied,
	 * optionally including deleted entities.
	 * <p>
	 * When {@code includeDeletions} is {@code false}, this
	 * behaves identically to {@link #find(Class, Object, Object)},
	 * returning {@code null} for DEL revisions. When {@code true},
	 * the entity state at deletion is returned instead of
	 * {@code null}.
	 *
	 * @param entityClass the audited entity class
	 * @param id the entity identifier
	 * @param changesetId the changeset identifier
	 * @param includeDeletions whether to include deleted entities
	 * @param <T> the entity type
	 *
	 * @return the entity state in that changeset
	 */
	<T> T find(Class<T> entityClass, Object id, Object changesetId, boolean includeDeletions);

	/**
	 * Find an entity snapshot as of the given instant. Returns
	 * the state at the highest revision on or before the instant.
	 *
	 * @param entityClass the audited entity class
	 * @param id the entity identifier
	 * @param instant the point in time
	 * @param <T> the entity type
	 *
	 * @return the entity state, or {@code null}
	 */
	<T> T find(Class<T> entityClass, Object id, Instant instant);

	/**
	 * Find all entity snapshots of the given type that
	 * were modified in a specific changeset.
	 *
	 * @param entityClass the audited entity class
	 * @param changesetId the changeset identifier
	 * @param <T> the entity type
	 *
	 * @return the entity snapshots modified in that changeset
	 */
	<T> List<T> findEntitiesModifiedAt(Class<T> entityClass, Object changesetId);

	/**
	 * Find all entity snapshots of the given type that
	 * were modified in a specific changeset with the
	 * specified modification type.
	 *
	 * @param entityClass the audited entity class
	 * @param changesetId the changeset identifier
	 * @param modificationType the modification type filter
	 * @param <T> the entity type
	 *
	 * @return the matching entity snapshots
	 */
	<T> List<T> findEntitiesModifiedAt(Class<T> entityClass, Object changesetId, ModificationType modificationType);

	/**
	 * Find all entity snapshots of the given type that
	 * were modified in a specific changeset, grouped
	 * by modification type (ADD, MOD, DEL).
	 *
	 * @param entityClass the audited entity class
	 * @param changesetId the changeset identifier
	 * @param <T> the entity type
	 *
	 * @return entity snapshots grouped by modification type
	 */
	<T> Map<ModificationType, List<T>> findEntitiesGroupedByModificationType(
			Class<T> entityClass,
			Object changesetId);

	/**
	 * Get the full audit history for an entity, ordered
	 * chronologically by changeset identifier.
	 * <p>
	 * Each entry contains the entity snapshot, the changeset
	 * identifier (or changeset entity), and the
	 * {@linkplain ModificationType modification type}
	 * (ADD/MOD/DEL).
	 * <p>
	 * For DEL entries, the entity snapshot reflects the state
	 * at the moment of deletion.
	 *
	 * @param entityClass the audited entity class
	 * @param id the entity identifier
	 * @param <T> the entity type
	 *
	 * @return the audit history as a list of {@link AuditEntry}
	 */
	<T> List<AuditEntry<T>> getHistory(Class<T> entityClass, Object id);

	// --- Cross-type changeset queries ---

	/**
	 * Get the set of entity types that were modified in the
	 * given changeset.
	 * <p>
	 * Requires a {@link ChangesetEntity @ChangesetEntity} with a
	 * {@link ChangesetEntity.ModifiedEntities @ModifiedEntities} property
	 * (e.g. {@link DefaultTrackingModifiedEntitiesChangesetEntity}).
	 *
	 * @param changesetId the changeset identifier
	 *
	 * @return the set of entity classes modified in that changeset
	 *
	 * @throws AuditException if entity change tracking is not enabled
	 */
	Set<Class<?>> getEntityTypesModifiedAt(Object changesetId);

	/**
	 * Find all entity snapshots across all audited types that
	 * were modified in the given changeset.
	 * <p>
	 * Requires a {@link ChangesetEntity @ChangesetEntity} with a
	 * {@link ChangesetEntity.ModifiedEntities @ModifiedEntities} property
	 * (e.g. {@link DefaultTrackingModifiedEntitiesChangesetEntity}).
	 *
	 * @param changesetId the changeset identifier
	 *
	 * @return all entity snapshots modified in that changeset
	 *
	 * @throws AuditException if entity change tracking is not enabled
	 */
	List<Object> findAllEntitiesModifiedAt(Object changesetId);

	/**
	 * Find all entity snapshots across all audited types that
	 * were modified in the given changeset with the specified
	 * modification type.
	 * <p>
	 * Requires a {@link ChangesetEntity @ChangesetEntity} with a
	 * {@link ChangesetEntity.ModifiedEntities @ModifiedEntities} property
	 * (e.g. {@link DefaultTrackingModifiedEntitiesChangesetEntity}).
	 *
	 * @param changesetId the changeset identifier
	 * @param modificationType the modification type filter
	 *
	 * @return the matching entity snapshots
	 *
	 * @throws AuditException if entity change tracking is not enabled
	 */
	List<Object> findAllEntitiesModifiedAt(Object changesetId, ModificationType modificationType);

	/**
	 * Find all entity snapshots modified in the given changeset,
	 * grouped by modification type (ADD, MOD, DEL).
	 * <p>
	 * Requires a {@link ChangesetEntity @ChangesetEntity} with a
	 * {@link ChangesetEntity.ModifiedEntities @ModifiedEntities} property
	 * (e.g. {@link DefaultTrackingModifiedEntitiesChangesetEntity}).
	 *
	 * @param changesetId the changeset identifier
	 *
	 * @return entity snapshots grouped by modification type
	 *
	 * @throws AuditException if entity change tracking is not enabled
	 */
	Map<ModificationType, List<Object>> findAllEntitiesGroupedByModificationType(Object changesetId);

	/**
	 * Get the timestamp of a specific changeset. Requires
	 * a {@link ChangesetEntity @ChangesetEntity} with a
	 * {@link ChangesetEntity.Timestamp @Timestamp} field.
	 *
	 * @param changesetId the changeset identifier
	 *
	 * @return the changeset timestamp
	 *
	 * @throws AuditException if no changeset entity is configured
	 * or the changeset does not exist
	 */
	Instant getChangesetTimestamp(Object changesetId);

	/**
	 * Get the changeset identifier that was current at or
	 * before the given instant. Requires a
	 * {@link ChangesetEntity @ChangesetEntity} with a
	 * {@link ChangesetEntity.Timestamp @Timestamp} field.
	 *
	 * @param instant the point in time
	 *
	 * @return the most recent changeset identifier at or
	 * before the given instant
	 *
	 * @throws AuditException if no changeset exists at or
	 * before the given instant
	 */
	Object getChangesetId(Instant instant);

	/**
	 * Load the changeset entity for the given changeset identifier.
	 * Requires a {@link ChangesetEntity @ChangesetEntity}.
	 *
	 * @param changesetEntityClass the changeset entity class
	 * @param changesetId the changeset identifier
	 * @param <T> the changeset entity type
	 *
	 * @return the changeset entity
	 *
	 * @throws AuditException if no changeset entity is configured
	 * or the changeset does not exist
	 */
	<T> T findChangeset(Class<T> changesetEntityClass, Object changesetId);

	/**
	 * Load changeset entities for multiple changeset identifiers.
	 * Requires a {@link ChangesetEntity @ChangesetEntity}.
	 *
	 * @param changesetEntityClass the changeset entity class
	 * @param changesetIds the changeset identifiers
	 * @param <T> the changeset entity type
	 *
	 * @return a map from changeset identifier to changeset entity
	 */
	<T> Map<Object, T> findChangesets(Class<T> changesetEntityClass, Set<?> changesetIds);

}
