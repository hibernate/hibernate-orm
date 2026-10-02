package org.hibernate.event.spi;

/**
 * Event class for {@link org.hibernate.Session#clear}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.Session#clear
 */
public class ClearEvent extends AbstractSessionEvent {
	public ClearEvent(EventSource source) {
		super( source );
	}
}
