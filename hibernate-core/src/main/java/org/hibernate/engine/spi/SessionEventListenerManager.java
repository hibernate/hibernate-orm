/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.SessionEventListener;

/**
 * @author Steve Ebersole
 */
public interface SessionEventListenerManager extends SessionEventListener {
	void addListener(SessionEventListener... listeners);
}
