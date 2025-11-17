/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Loader for {@link org.hibernate.annotations.NaturalId} handling
 *
 * @author Steve Ebersole
 */
public interface NaturalIdLoader<T> extends EntityLoader, MultiKeyLoader {

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
	T load(Object naturalIdToLoad, NaturalIdLoadOptions options, SharedSessionContractImplementor session);

	/**
	 * Resolve the id from natural-id value
	 */
	Object resolveNaturalIdToId(Object naturalIdValue, SharedSessionContractImplementor session);

	/**
	 * Resolve the natural-id value(s) from an id
	 */
	Object resolveIdToNaturalId(Object id, SharedSessionContractImplementor session);
}
