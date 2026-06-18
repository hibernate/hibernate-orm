/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import org.hibernate.Incubating;

/**
 * Extension to {@link TupleTransformer} exposing the transformation target type.
 *
 * @apiNote This is mainly intended for use in equality checking while applying
 *          result deduplication for queries.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface TypedTupleTransformer<T> extends TupleTransformer<T> {
	/**
	 * The type resulting from this transformation
	 */
	Class<T> getTransformedType();
}
