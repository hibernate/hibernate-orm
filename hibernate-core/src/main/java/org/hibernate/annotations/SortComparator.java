/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Comparator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Sort a {@link java.util.Set} or {@link java.util.Map} using the given {@link Comparator}.
 * <p>
 * Sorting is performed in memory, by Java's {@link java.util.TreeSet} or {@link java.util.TreeMap},
 * and is maintained by any operation that mutates the collection.
 * <ul>
 * <li>Use {@link SortNatural} to sort by {@linkplain java.util.Comparator natural order}.
 * <li>Use {@link jakarta.persistence.OrderBy} to order using an expression written in HQL.
 * <li>Use {@link SQLOrder} to order using an expression written in native SQL.
 * </ul>
 * <p>
 * It is illegal to use both {@code SortComparator} and {@link SortNatural}.
 *
 * @see SortComparator
 * @see jakarta.persistence.OrderBy
 * @see SQLOrder
 *
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface SortComparator {
	/**
	 * A class which implements {@link Comparator Comparator&lt;E&gt;} where {@code E} is the element type.
	 */
	Class<? extends Comparator<?>> value();
}
