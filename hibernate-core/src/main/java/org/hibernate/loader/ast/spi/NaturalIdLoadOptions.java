/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.LockOptions;

/**
 * Options for loading by natural-id
 */
public interface NaturalIdLoadOptions {
	/**
	 * Singleton access
	 */
	NaturalIdLoadOptions NONE = new NaturalIdLoadOptions() {
		@Override
		public LockOptions getLockOptions() {
			return null;
		}

		@Override
		public boolean isSynchronizationEnabled() {
			return false;
		}
	};

	/**
	 * The locking options for the loaded entity
	 */
	LockOptions getLockOptions();

	/**
	 * Whether Hibernate should perform "synchronization" prior to performing
	 * look-ups?
	 */
	boolean isSynchronizationEnabled();
}
