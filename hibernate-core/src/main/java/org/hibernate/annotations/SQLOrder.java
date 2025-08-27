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
 * Order a collection using an expression or list of expression written
 * in native SQL. For example, {@code @SQLOrder("first_name, last_name")},
 * {@code @SQLOrder("char_length(name) desc")}, or even
 * {@code @SQLOrder("name asc nulls last")}.
 * <p>
 * The order is applied by the database when the collection is fetched,
 * but is not maintained by operations that mutate the collection in
 * memory.
 * <p>
 * If the collection is a {@link java.util.Set} or {@link java.util.Map},
 * the order is maintained using a {@link java.util.LinkedHashSet} or
 * {@link java.util.LinkedHashMap}. If the collection is a bag or
 * {@link java.util.List}, the order is maintained by the underlying
 * {@link java.util.ArrayList}.
 * <p>
 * There are several other ways to order or sort a collection:
 * <ul>
 * <li>Use the JPA-defined {@link jakarta.persistence.OrderBy} annotation
 *     to order using an expression written in HQL/JPQL. Since HQL is more
 *     portable between databases, this is the preferred alternative most
 *     of the time.
 * <li>Use {@link SortComparator} to sort the collection in memory using
 *     a {@link java.util.Comparator}, or {@link SortNatural} to sort the
 *     collection in memory according to its {@linkplain java.util.Comparator
 *     natural order}.
 * <li>Use {@link jakarta.persistence.OrderColumn} to maintain the order
 *     of a {@link java.util.List} with a dedicated index column.
 * </ul>
 * <p>
 * It's illegal to use {@code SQLOrder} together with the JPA-defined
 * {@link jakarta.persistence.OrderBy} for the same collection.
 *
 * @see jakarta.persistence.OrderBy
 * @see SortComparator
 * @see SortNatural
 *
 * @since 6.3
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 *
 * @see DialectOverride.SQLOrder
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface SQLOrder {
	/**
	 * A comma-separated list native SQL expressions used to sort the
	 * collection elements. Each element of the list may optionally
	 * specify:
	 * <ul>
	 * <li>{@code asc}-ending or {@code desc}-ending order, or even
	 * <li>{@code nulls first} or {@code nulls last}.
	 * </ul>
	 * Hibernate does not interpret these keywords, and simply passes
	 * them through to the generated SQL.
	 */
	String value();
}
