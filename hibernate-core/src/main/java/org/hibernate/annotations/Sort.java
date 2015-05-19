/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Collection sort (in-memory sorting).  Different that ordering, which is applied during the SQL select.
 *
 * @author Emmanuel Bernard
 *
 * @see OrderBy
 *
 * @deprecated Use {@link SortComparator} or {@link SortNatural} instead depending on need.
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Deprecated
public @interface Sort {
	/**
	 * The type of sorting to use.  The default is to not use sorting.
	 */
	SortType type() default SortType.UNSORTED;

	/**
	 * Specifies the comparator to use.  Only valid when {@link #type} specifies {@link SortType#COMPARATOR}.
	 *
	 * TODO find a way to use Class<Comparator> -> see HHH-8164
	 */
	Class comparator() default void.class;
}
