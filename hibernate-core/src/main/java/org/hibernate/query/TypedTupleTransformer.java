/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

/**
 * Extension to TupleTransformer exposing the transformation target type.
 *
 * @apiNote This is mainly intended for use in equality checking while applying
 * result de-duplication for queries.
 *
 * @author Steve Ebersole
 */
public interface TypedTupleTransformer<T> extends TupleTransformer<T> {
	/**
	 * The type resulting from this transformation
	 */
	Class<T> getTransformedType();
}
