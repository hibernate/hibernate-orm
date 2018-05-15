/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

/**
 * User hook for applying transformations of the query result tuples (the result "row").
 *
 * Ultimately, gets wrapped in a
 * {@link org.hibernate.sql.exec.internal.RowTransformerTupleTransformerAdapter}
 * to adapt the TupleTransformer to the {@link org.hibernate.sql.exec.spi.RowTransformer}
 * contract, which is the thing actually used to process the results internally.
 *
 * Note that {@link JpaTupleTransformer} is a special sub-type applications may use
 * to transform the row into a JPA {@link javax.persistence.Tuple}.  JpaTupleTransformer is
 * deprecated as it is much more appropriate (and simpler) to simply specify the Query
 * return type as Tuple
 *
 * @see org.hibernate.transform.ResultTransformer
 * @see org.hibernate.sql.exec.spi.RowTransformer
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
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

	/**
	 * How many result elements will this transformation produce?
	 */
	default int determineNumberOfResultElements(int rawElementCount) {
		return rawElementCount;
	}
}
