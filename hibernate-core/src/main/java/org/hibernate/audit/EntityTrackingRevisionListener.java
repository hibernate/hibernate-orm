/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;


/**
 * A callback invoked for each entity change within a transaction,
 * in addition to the {@link RevisionListener#newRevision} callback
 * invoked once when the revision entity is created.
 *
 * @author Marco Belladelli
 * @since 7.4
 */
public interface EntityTrackingRevisionListener extends RevisionListener {
	/**
	 * Called for each entity change within a transaction, after
	 * audit rows are written. The revision entity has already
	 * been persisted at this point.
	 *
	 * @param entityClass the entity class
	 * @param entityId the entity identifier
	 * @param modificationType the type of change (ADD, MOD, DEL)
	 * @param revisionEntity the revision entity instance, or
	 * {@code null} if no revision entity is configured
	 */
	void entityChanged(
			Class<?> entityClass,
			Object entityId,
			ModificationType modificationType,
			Object revisionEntity);
}
