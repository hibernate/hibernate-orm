/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Order a collection using an expression written in native SQL.
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
 * It's illegal to use {@code OrderBy} together with the JPA-defined
 * {@link jakarta.persistence.OrderBy} for the same collection.
 *
 * @see jakarta.persistence.OrderBy
 * @see SortComparator
 * @see SortNatural
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 *
 * @see DialectOverride.OrderBy
 *
 * @deprecated Use {@link SQLOrder} instead. This annotation will be
 *             removed eventually, since its unqualified name collides
 *             with {@link jakarta.persistence.OrderBy}.
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Deprecated(since = "6.3", forRemoval = true)
public @interface OrderBy {
	/**
	 * The native SQL expression used to sort the collection elements.
	 */
	String clause();
}
