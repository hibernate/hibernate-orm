package org.hibernate.event.spi;

/**
 * Called after an entity update is committed to the datastore.
 *
 * @author Shawn Clowater
 */
public interface PostCommitUpdateEventListener extends PostUpdateEventListener {
	/**
	 * Called when a commit fails and an an entity was scheduled for update
	 * 
	 * @param event the update event to be handled
	 */
	public void onPostUpdateCommitFailed(PostUpdateEvent event);
}
