/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * query result as a whole.  This might be useful to convert from
	 * one collection type to another or to remove duplicates from the
	 * result, etc.
	 *
	 * @param resultList The result list as would otherwise be returned from
	 * the Query without the intervention of this ResultListTransformer
	 *
	 * @return The transformed result.
	 */
	List<T> transformList(List<T> resultList);
}
