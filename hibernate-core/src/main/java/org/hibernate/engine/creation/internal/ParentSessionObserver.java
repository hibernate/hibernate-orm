/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import org.hibernate.engine.creation.CommonSharedBuilder;

/**
 * Allows observation of flush and closure events of a parent session from a
 * child session which shares connection/transaction with the parent.
 *
 * @see CommonSharedBuilder#connection()
 *
 * @author Steve Ebersole
 */
public interface ParentSessionObserver {
	/**
	 * Callback when the parent is flushed.  Used to flush the child session.
	 */
	void onParentFlush();

	/**
	 * Callback when the parent is closed.  Used to close the child session.
	 */
	void onParentClose();
}
