/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.engine.spi.SessionImplementor;

/// Support for callbacks after the execution of a FlushOperation.
///
/// @see FlushOperation#getPostExecutionCallback()
///
/// @author Steve Ebersole
public interface PostExecutionCallback {
	/// The callback.
	void handle(SessionImplementor session);
}
