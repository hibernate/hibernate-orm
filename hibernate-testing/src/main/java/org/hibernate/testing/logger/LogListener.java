/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.logger;

import org.jboss.logging.Logger.Level;

public interface LogListener {
	void loggedEvent(Level level, String renderedMessage, Throwable thrown);
}
