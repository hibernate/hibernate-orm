/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers;


/**
 * An implementation of this class, having a no-arg constructor, should be passed as an argument to the
 * {@link RevisionEntity} annotation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public interface RevisionListener {
	/**
	 * Called when a new revision is created.
	 *
	 * @param revisionEntity An instance of the entity annotated with {@link RevisionEntity}, which will be persisted
	 * after this method returns. All properties on this entity that are to be persisted should be set by this method.
	 */
	void newRevision(Object revisionEntity);
}
