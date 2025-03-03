/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Called after an entity delete is committed to the datastore.
 *
 * @author Shawn Clowater
 */
public interface PostCommitDeleteEventListener extends PostDeleteEventListener {
	/**
	 * Called when a commit fails and an entity was scheduled for deletion
	 *
	 * @param event the delete event to be handled
	 */
	void onPostDeleteCommitFailed(PostDeleteEvent event);
}
