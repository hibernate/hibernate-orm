/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;


import org.hibernate.annotations.Changelog;

/**
 * A callback invoked when a new
 * {@linkplain Changelog
 * changelog entity} is created, allowing the application to
 * populate custom fields such as the current user or a comment.
 *
 * @author Marco Belladelli
 * @see Changelog
 * @since 7.4
 */
public interface ChangesetListener {
	/**
	 * Called when a new changelog entity is created, before
	 * it is persisted. The implementation should set any
	 * custom properties on the changelog entity.
	 *
	 * @param changelog the new changelog entity instance
	 */
	void newChangeset(Object changelog);
}
