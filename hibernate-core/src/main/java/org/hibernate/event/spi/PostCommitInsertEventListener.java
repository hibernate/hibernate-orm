/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Called after an entity insert is committed to the datastore.
 *
 * @author Shawn Clowater
 */
public interface PostCommitInsertEventListener extends PostInsertEventListener {
	/**
	 * Called when a commit fails and an entity was scheduled for insertion
	 *
	 * @param event the insert event to be handled
	 */
	void onPostInsertCommitFailed(PostInsertEvent event);
}
