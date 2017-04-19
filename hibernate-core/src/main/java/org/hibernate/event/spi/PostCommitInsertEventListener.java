/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

/**
 * Called after an entity insert is committed to the datastore.
 *
 * @author Shawn Clowater
 */
public interface PostCommitInsertEventListener extends PostInsertEventListener {
	/**
	 * Called when a commit fails and an an entity was scheduled for insertion
	 * 
	 * @param event the insert event to be handled
	 */
	public void onPostInsertCommitFailed(PostInsertEvent event);
}
