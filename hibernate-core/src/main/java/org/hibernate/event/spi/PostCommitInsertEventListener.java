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
