//$Id: FlushEvent.java 6929 2005-05-27 03:54:08Z oneovthafew $
package org.hibernate.event;


/** 
 * Defines an event class for the flushing of a session.
 *
 * @author Steve Ebersole
 */
public class FlushEvent extends AbstractEvent {
	
	public FlushEvent(EventSource source) {
		super(source);
	}

}
