/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Loader for entities by multiple natural-ids
 *
 * @param <E> The entity Java type
 */
public interface MultiNaturalIdLoader<E> extends EntityMultiLoader<E> {
	/**
	 * Load multiple entities by natural-id.  The exact result depends on the passed options.
	 *
	 * @param naturalIds The natural-ids to load.  The values of this array will depend on whether the
	 * natural-id is simple or complex.
	 *
	 * @param <K> The basic form for a natural-id is a Map of its attribute values, or an array of the
	 * values positioned according to "attribute ordering".  Simple natural-ids can also be expressed
	 * by their simple (basic/embedded) type.
	 */
	<K> List<E> multiLoad(K[] naturalIds, MultiNaturalIdLoadOptions options, SharedSessionContractImplementor session);
}
