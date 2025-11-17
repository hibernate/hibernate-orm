/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;


import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Maps an entity to a database view. The name of the view is
 * determined according to the usual rules regarding table
 * mappings, and may be customized using the JPA-standard
 * {@link jakarta.persistence.Table @Table} annotation. This
 * annotation specifies the query which defines the view,
 * allowing the view to be exported by the schema management
 * tooling.
 * <p>
 * For example, this mapping:
 * <pre>
 * &#64;Immutable &#64;Entity
 * &#64;Table(name="summary")
 * &#64;View(query="""
 *             select type, sum(amount) as total, avg(amount) as average
 *             from details
 *             group by type
 *             """)
 * &#64;Synchronize("details")
 * public class Summary {
 *     &#64;Id String type;
 *     Double total;
 *     Double average;
 * }
 * </pre>
 * results in the following generated DDL:
 * <pre>
 * create view summary
 * as select type, sum(amount) as total, avg(amount) as average
 *    from details
 *    group by type
 * </pre>
 * <p>
 * If a view is not updatable, we recommend annotating the
 * entity {@link Immutable @Immutable}.
 * <p>
 * It's possible to have an entity class which maps a table,
 * and another entity which maps a view defined as a query
 * against that table. In this case, a stateful session is
 * vulnerable to data aliasing effects, and it's the
 * responsibility of client code to ensure that changes to
 * the first entity are flushed to the database before
 * reading the same data via the second entity. The
 * {@link Synchronize @Synchronize} annotation can help
 * alleviate this problem, but it's an incomplete solution.
 * <p>
 * Therefore, we recommend the use of {@linkplain
 * org.hibernate.StatelessSession stateless sessions}
 * when interacting with entities mapped to views.
 *
 * @since 6.3
 *
 * @author Gavin King
 *
 * @see Synchronize
 */
@Incubating
@Target(TYPE)
@Retention(RUNTIME)
public @interface View {
	/**
	 * The SQL query that defines the view.
	 */
	String query();
}
