//$Id$
package org.hibernate.event;


/** Defines an event class for the dirty-checking of a session.
 *
 * @author Steve Ebersole
 */
public class DirtyCheckEvent extends FlushEvent {
	
	private boolean dirty;

	public DirtyCheckEvent(EventSource source) {
		super(source);
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

}
