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
 * Order a collection using an expression written in native SQL.
 * <p>
 * The order is applied by the database when the collection is fetched, but is not maintained
 * by operations that mutate the collection in memory. If the collection is a {@link java.util.Set}
 * or {@link java.util.Map}, the order is maintained using a {@link java.util.LinkedHashSet} or
 * {@link java.util.LinkedHashMap}.
 * <ul>
 * <li>Use {@link jakarta.persistence.OrderBy} to order using an expression written in HQL.
 * <li>Use {@link SortComparator} to sort the collection in memory using a {@link java.util.Comparator}.
 * <li>Use {@link SortNatural} to sort the collection in its {@link java.util.Comparator natural order}.
 * <li>Use {@link jakarta.persistence.OrderColumn} to maintain the order of a {@link java.util.List}
 *     with a dedicated index column.
 * </ul>
 * <p>
 * It is illegal to use both {@code OrderBy} and {@link jakarta.persistence.OrderBy}.
 *
 * @see jakarta.persistence.OrderBy
 * @see SortComparator
 * @see SortNatural
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface OrderBy {
	/**
	 * The native SQL expression used to sort the collection elements.
	 */
	String clause();
}
