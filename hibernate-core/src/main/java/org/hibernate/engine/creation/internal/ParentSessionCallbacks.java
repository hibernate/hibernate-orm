/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

/**
 * Callbacks from a parent session to a child session for certain events.
 *
 * @author Steve Ebersole
 */
public interface ParentSessionCallbacks {
	/**
	 * Callback when the parent is flushed.
	 */
	void onParentFlush();

	/**
	 * Callback when the parent is closed.
	 */
	void onParentClose();
}
