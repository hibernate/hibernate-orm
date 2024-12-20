/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
