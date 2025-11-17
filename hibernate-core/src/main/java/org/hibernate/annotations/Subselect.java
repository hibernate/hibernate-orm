/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Maps an {@linkplain Immutable immutable} and read-only entity to a given
 * SQL {@code select} expression.
 * <p>
 * For example:
 * <pre>
 * &#64;Immutable &#64;Entity
 * &#64;Subselect("""
 *            select type, sum(amount) as total, avg(amount) as average
 *            from details
 *            group by type
 *            """)
 * &#64;Synchronize("details")
 * public class Summary {
 *     &#64;Id String type;
 *     Double total;
 *     Double average;
 * }
 * </pre>
 * <p>
 * This is an alternative to defining a {@linkplain View view} and mapping
 * the entity to the view using the {@link jakarta.persistence.Table @Table}
 * annotation.
 * <p>
 * It's possible to have an entity class which maps a table, and another
 * entity which is defined by a {@code @Subselect} involving the same table.
 * In this case, a stateful session is vulnerable to data aliasing effects,
 * and it's the responsibility of client code to ensure that changes to the
 * first entity are flushed to the database before reading the same data via
 * the second entity. The {@link Synchronize @Synchronize} annotation can
 * help alleviate this problem, but it's an incomplete solution. We therefore
 * recommend the use of {@linkplain org.hibernate.StatelessSession stateless
 * sessions} in this situation.
 *
 * @see Synchronize
 * @see View
 *
 * @author Sharath Reddy
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Subselect {
	/**
	 * The subquery, written in native SQL.
	 */
	String value();
}
