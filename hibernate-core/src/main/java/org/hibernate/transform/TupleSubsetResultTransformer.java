/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.transform;


/**
 * A ResultTransformer that operates on "well-defined" and consistent
 * subset of a tuple's elements.
 *
 * "Well-defined" means that:
 * <ol>
 *     <li>
 *         the indexes of tuple elements accessed by a
 *         TupleSubsetResultTransformer depends only on the aliases
 *         and the number of elements in the tuple; i.e, it does
 *         not depend on the value of the tuple being transformed;
 *     </li>
 *     <li>
 *         any tuple elements included in the transformed value are
 *         unmodified by the transformation;
 *     </li>
 *     <li>
 *         transforming equivalent tuples with the same aliases multiple
 *         times results in transformed values that are equivalent;
 *     </li>
 *     <li>
 *         the result of transforming the tuple subset (only those
 *         elements accessed by the transformer) using only the
 *         corresponding aliases is equivalent to transforming the
 *         full tuple with the full array of aliases;
 *     </li>
 *     <li>
 *         the result of transforming a tuple with non-accessed tuple
 *         elements and corresponding aliases set to null
 *         is equivalent to transforming the full tuple with the
 *         full array of aliases;
 *     </li>
 * </ol>
 *
 * @author Gail Badner
 */
public interface TupleSubsetResultTransformer extends ResultTransformer {
	/**
	 * When a tuple is transformed, is the result a single element of the tuple?
	 *
	 * @param aliases - the aliases that correspond to the tuple
	 * @param tupleLength - the number of elements in the tuple
	 * @return true, if the transformed value is a single element of the tuple;
	 *         false, otherwise.
	 */
	boolean isTransformedValueATupleElement(String[] aliases, int tupleLength);

	/**
	 * Returns an array with the i-th element indicating whether the i-th
	 * element of the tuple is included in the transformed value.
	 *
	 * @param aliases - the aliases that correspond to the tuple
	 * @param tupleLength - the number of elements in the tuple
	 * @return array with the i-th element indicating whether the i-th
	 *         element of the tuple is included in the transformed value.
	 */
	boolean[] includeInTransform(String[] aliases, int tupleLength);
}
