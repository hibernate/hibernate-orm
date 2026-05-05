/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.bind;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionImplementor;

/// Support for callbacks after the execution of a FlushOperation.
///
/// @see FlushOperation#getPostExecutionCallback()
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public interface PostExecutionCallback {
	/// The callback.
	void handle(SessionImplementor session);
}
