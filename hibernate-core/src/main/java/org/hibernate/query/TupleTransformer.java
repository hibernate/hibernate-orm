/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query;

/**
 * Defines a strategy for free-form transformations of the query result
 * tuples (the result "row").
 * <p/>
 * To be determined : the effect between TupleTransformer, result-type and dynamic-instantiations
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
}
