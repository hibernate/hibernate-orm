/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.transform;

import java.io.Serializable;
import java.util.List;

import org.hibernate.query.Query;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;

/**
 * Implementors define a strategy for transforming query results into the
 * actual application-visible query result list.
 *
 * @see Query#setResultTransformer(ResultTransformer)
 *
 * @author Gavin King
 *
 * @deprecated Use {@link TupleTransformer} and/or {@link ResultListTransformer} instead
 */
@Deprecated(since = "6.0")
public interface ResultTransformer<T> extends TupleTransformer<T>, ResultListTransformer<T>, Serializable {
	@Override
	default List<T> transformList(List<T> resultList) {
		return resultList;
	}
}
