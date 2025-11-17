/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

/**
 * Lifecycle contract for services which wish to be notified when it is time to stop.
 *
 * @author Steve Ebersole
 */
public interface Stoppable {
	/**
	 * Stop phase notification
	 */
	void stop();
}
