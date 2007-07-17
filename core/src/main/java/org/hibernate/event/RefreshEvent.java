//$Id: RefreshEvent.java 6929 2005-05-27 03:54:08Z oneovthafew $
package org.hibernate.event;

import org.hibernate.LockMode;

/**
 *  Defines an event class for the refreshing of an object.
 *
 * @author Steve Ebersole
 */
public class RefreshEvent extends AbstractEvent {

	private Object object;
	private LockMode lockMode = LockMode.READ;

	public RefreshEvent(Object object, EventSource source) {
		super(source);
		if (object == null) {
			throw new IllegalArgumentException("Attempt to generate refresh event with null object");
		}
		this.object = object;
	}

	public RefreshEvent(Object object, LockMode lockMode, EventSource source) {
		this(object, source);
		if (lockMode == null) {
			throw new IllegalArgumentException("Attempt to generate refresh event with null lock mode");
		}
		this.lockMode = lockMode;
	}

	public Object getObject() {
		return object;
	}

	public LockMode getLockMode() {
		return lockMode;
	}
}
