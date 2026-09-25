package org.hibernate.engine.spi;

import org.hibernate.SessionEventListener;

/**
 * @author Steve Ebersole
 */
public interface SessionEventListenerManager extends SessionEventListener {
	void addListener(SessionEventListener... listeners);
}
