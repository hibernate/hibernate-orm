/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import org.hibernate.Incubating;
import org.hibernate.sql.results.internal.RowTransformerTupleTransformerAdapter;

import java.util.List;

/**
 * Defines some transformation applied to each result of a {@link Query} before
 * the results are packaged as a {@link List} and returned to the caller. Each
 * result is received as a tuple (that is, as an {@code Object[]}), and may be
 * transformed to some other type.
 *
 * @implNote Every {@code TupleTransformer} is automatically wrapped in an
 * instance of {@link RowTransformerTupleTransformerAdapter}, adapting it to
 * the {@link org.hibernate.sql.results.spi.RowTransformer} contract, which is
 * always used to actually process the results internally.
 *
 * @see Query#setTupleTransformer
 * @see Query#list
 * @see Query#getResultList
 * @see ResultListTransformer
 * @see org.hibernate.sql.results.spi.RowTransformer
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
@Incubating
@FunctionalInterface
public interface TupleTransformer<T> {
	/**
	 * Tuples are the elements making up each "row" of the query result.
	 * The contract here is to transform these elements into the final
	 * row shape.
	 *
	 * @param tuple The result elements
	 * @param aliases The result aliases ("parallel" array to tuple)
	 *
	 * @return The transformed row.
	 */
	T transformTuple(Object[] tuple, String[] aliases);
}
