/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
