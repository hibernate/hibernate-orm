package org.hibernate.service.spi;


/**
 * Lifecycle contract for services which wish to be notified when it is time to start.
 *
 * @author Steve Ebersole
 */
public interface Startable {
	/**
	 * Start phase notification
	 */
	void start();
}
