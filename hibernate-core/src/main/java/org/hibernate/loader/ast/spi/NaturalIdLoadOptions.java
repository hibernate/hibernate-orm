package org.hibernate.loader.ast.spi;

import jakarta.annotation.Nullable;

import org.hibernate.LockOptions;

/**
 * Options for loading by natural-id
 */
public interface NaturalIdLoadOptions {
	/**
	 * Singleton access
	 */
	NaturalIdLoadOptions NONE = new NaturalIdLoadOptions() {
		@Nullable
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
	@Nullable
	LockOptions getLockOptions();

	/**
	 * Whether Hibernate should perform "synchronization" prior to performing
	 * look-ups?
	 */
	boolean isSynchronizationEnabled();
}
