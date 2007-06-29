//$Id$
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
