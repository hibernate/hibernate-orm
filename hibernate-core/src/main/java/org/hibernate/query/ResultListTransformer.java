/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import java.util.List;

import org.hibernate.Incubating;

/**
 * Defines some processing performed on the overall result {@link List}
 * of a {@link Query} before the result list is returned to the caller.
 *
 * @see Query#setResultListTransformer
 * @see Query#list
 * @see Query#getResultList
 * @see TupleTransformer
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
@Incubating
@FunctionalInterface
public interface ResultListTransformer<T> {
	/**
	 * Here we have an opportunity to perform transformation on the
	 * query result as a whole. This might be useful to convert from
	 * one collection type to another or to remove duplicates from the
	 * result, etc.
	 *
	 * @param resultList The result list as would otherwise be returned
	 *                   by the {@code Query} without the intervention
	 *                   of this {@code ResultListTransformer}
	 *
	 * @return The transformed result.
	 */
	List<T> transformList(List<T> resultList);
}
