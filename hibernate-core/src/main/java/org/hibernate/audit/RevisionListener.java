/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;


/**
 * A callback invoked when a new revision entity is created,
 * allowing the application to populate custom fields such as
 * the current user or a comment.
 *
 * @author Marco Belladelli
 * @since 7.4
 */
public interface RevisionListener {
	/**
	 * Called when a new revision entity is created, before it
	 * is persisted. The implementation should set any custom
	 * properties on the revision entity.
	 *
	 * @param revisionEntity the revision entity instance
	 */
	void newRevision(Object revisionEntity);
}
