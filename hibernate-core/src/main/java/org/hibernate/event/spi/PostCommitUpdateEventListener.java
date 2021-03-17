/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

/**
 * Called after an entity update is committed to the datastore.
 *
 * @author Shawn Clowater
 */
public interface PostCommitUpdateEventListener extends PostUpdateEventListener {
	/**
	 * Called when a commit fails and an entity was scheduled for update
	 * 
	 * @param event the update event to be handled
	 */
	public void onPostUpdateCommitFailed(PostUpdateEvent event);
}
