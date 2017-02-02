/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Possible collection sorting strategies.
 *
 * @author Emmanuel Bernard
 *
 * @deprecated Since {@link Sort} is deprecated.
 */
@Deprecated
public enum SortType {
	/**
	 * The collection is unsorted.
	 */
	UNSORTED,
	/**
	 * The collection is sorted using its natural sorting.
	 */
	NATURAL,
	/**
	 * The collection is sorted using a supplied comparator.
	 */
	COMPARATOR
}
