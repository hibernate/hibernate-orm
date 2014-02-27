package org.hibernate.event.spi;

/**
 * Called after an entity delete is committed to the datastore.
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
