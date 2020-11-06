/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.spi;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Loader for {@link org.hibernate.annotations.NaturalId} handling
 *
 * @author Steve Ebersole
 */
public interface NaturalIdLoader<T> extends Loader {
	/**
	 * Options for the {@link #load} method
	 */
	interface LoadOptions {
		/**
		 * Singleton access
		 */
		LoadOptions NONE = new LoadOptions() {
			@Override
			public LockOptions getLockOptions() {
				return LockOptions.READ;
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

	/**
	 * Perform the load of the entity by its natural-id
	 *
	 * @param naturalIdToLoad The natural-id to load.  One of 2 forms accepted:
	 *		* Single-value - valid for entities with a simple (single-valued)
	 *			natural-id
	 *		* Map - valid for any natural-id load.  The map is each value keyed
	 *			by the attribute name that the value corresponds to.  Even though
	 *			this form is allowed for simple natural-ids, the single value form
	 *			should be used as it is more efficient
	 * @param options The options to apply to the load operation
	 * @param session The session into which the entity is being loaded
	 */
	T load(Object naturalIdToLoad, LoadOptions options, SharedSessionContractImplementor session);

	/**
	 * Resolve the id from natural-id value
	 */
	Object resolveNaturalIdToId(Object naturalIdValue, SharedSessionContractImplementor session);

	/**
	 * Resolve the natural-id value(s) from an id
	 */
	Object resolveIdToNaturalId(Object id, SharedSessionContractImplementor session);
}
