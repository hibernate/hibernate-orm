/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.spi;


/**
 * An observer contract for batch events.
 *
 * @author Steve Ebersole
 */
public interface BatchObserver {
	/**
	 * Indicates explicit execution of the batch via a call to {@link Batch#execute()}.
	 */
	void batchExplicitlyExecuted();

	/**
	 * Indicates an implicit execution of the batch.
	 */
	void batchImplicitlyExecuted();
}
