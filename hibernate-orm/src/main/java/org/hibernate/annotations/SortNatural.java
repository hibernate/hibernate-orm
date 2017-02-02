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
 * Specifies in-memory Set/Map sorting using natural sorting.
 *
 * NOTE : Sorting is different than ordering (see {@link OrderBy}) which is applied during the SQL SELECT.
 *
 * For sorting based on a comparator, use {@link SortComparator} instead.  It is illegal to combine
 *{@link SortComparator} and SortNatural.
 *
 * @see OrderBy
 * @see SortComparator
 *
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface SortNatural {
}
