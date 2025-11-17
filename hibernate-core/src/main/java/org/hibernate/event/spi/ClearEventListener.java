/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Listener for notification of {@link org.hibernate.Session#clear()}
 *
 * @author Steve Ebersole
 */
public interface ClearEventListener {
	/**
	 * Callback for {@link org.hibernate.Session#clear()} notification
	 *
	 * @param event The event representing the clear
	 */
	void onClear(ClearEvent event);
}
