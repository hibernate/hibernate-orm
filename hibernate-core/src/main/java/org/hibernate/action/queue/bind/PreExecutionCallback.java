/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.bind;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Callback invoked immediately before executing a planned operation.
 *
 * @author Steve Ebersole
 */
public interface PreExecutionCallback {
	/**
	 * @return {@code true} to execute the operation; {@code false} to skip it.
	 */
	boolean beforeExecution(SessionImplementor session);
}
