/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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
