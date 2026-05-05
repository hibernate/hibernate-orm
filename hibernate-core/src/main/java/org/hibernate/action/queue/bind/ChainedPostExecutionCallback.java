/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.bind;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Steve Ebersole
 */
public class ChainedPostExecutionCallback implements PostExecutionCallback {
	private final PostExecutionCallback first;
	private final PostExecutionCallback second;

	public ChainedPostExecutionCallback(PostExecutionCallback first, PostExecutionCallback second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public void handle(SessionImplementor session) {
		first.handle(session);
		second.handle(session);
	}
}
