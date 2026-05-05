/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;


/**
 * A callback invoked when a new
 * {@linkplain org.hibernate.annotations.ChangesetEntity
 * changeset entity} is created, allowing the application to
 * populate custom fields such as the current user or a comment.
 *
 * @see org.hibernate.annotations.ChangesetEntity
 *
 * @author Marco Belladelli
 * @since 7.4
 */
public interface ChangesetListener {
	/**
	 * Called when a new changeset entity is created, before it
	 * is persisted. The implementation should set any custom
	 * properties on the changeset entity.
	 *
	 * @param changesetEntity the new instance of the changeset
	 *                        entity
	 */
	void newChangeset(Object changesetEntity);
}
