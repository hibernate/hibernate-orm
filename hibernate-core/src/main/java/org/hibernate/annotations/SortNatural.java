/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Sort a {@link java.util.Set} or {@link java.util.Map} in its {@link Comparable natural order}
 * <p>
 * Sorting is performed in memory, by Java's {@link java.util.TreeSet} or {@link java.util.TreeMap},
 * and is maintained by any operation that mutates the collection.
 * <ul>
 * <li>Use {@link SortComparator} to sort the collection in memory using a {@link java.util.Comparator}.
 * <li>Use {@link jakarta.persistence.OrderBy} to order using an expression written in HQL.
 * <li>Use {@link SQLOrder} to order using an expression written in native SQL.
 * </ul>
 * <p>
 * It is illegal to use both {@code SortNatural} and {@link SortComparator}.
 *
 * @see SortComparator
 * @see jakarta.persistence.OrderBy
 * @see SQLOrder
 *
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface SortNatural {
}
