/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.sql.results.internal.RowTransformerTupleTransformerAdapter;

import java.util.List;

/**
 * Defines some transformation applied to each result of a {@link org.hibernate.Query}
 * before the results are packaged as a {@link List} and returned to the caller of
 * {@link org.hibernate.Query#getResultList()}. Each query result is received as a
 * tuple, that is, as an array of type {@code Object[]}, and may be transformed to
 * some other type.
 * <p>
 * Every {@code TupleTransformer} is automatically wrapped in an instance of
 * {@link RowTransformerTupleTransformerAdapter}, adapting it to the
 * {@link org.hibernate.sql.results.spi.RowTransformer} contract, which is always
 * used to actually process the results internally.
 *
 * @see org.hibernate.transform.ResultTransformer
 * @see org.hibernate.sql.results.spi.RowTransformer
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
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
