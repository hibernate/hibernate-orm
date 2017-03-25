/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

/**
 * Called afterQuery an entity delete is committed to the datastore.
 *
 * @author Shawn Clowater
 */
public interface PostCommitDeleteEventListener extends PostDeleteEventListener {
	/**
	 * Called when a commit fails and an an entity was scheduled for deletion
	 * 
	 * @param event the delete event to be handled
	 */
	public void onPostDeleteCommitFailed(PostDeleteEvent event);
}
