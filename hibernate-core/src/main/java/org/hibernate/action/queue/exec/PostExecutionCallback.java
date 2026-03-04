/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Steve Ebersole
 */
public interface PostExecutionCallback {
	void handle(SessionImplementor session);
}
