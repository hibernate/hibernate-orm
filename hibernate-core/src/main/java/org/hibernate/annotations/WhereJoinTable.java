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
 * Specifies a restriction written in native SQL to add to the generated SQL
 * when querying the {@linkplain jakarta.persistence.JoinTable join table}
 * of a collection.
 * <p>
 * For example, <code>&#64;WhereJoinTable("status &lt;&gt; 'DELETED'")</code>
 * could be used to hide associations which have been soft-deleted from an
 * association table.
 *
 * @apiNote This separate annotation is useful because it's possible to filter
 *          a many-to-many association <em>both</em> by a restriction on the
 *          join table, and, <em>simultaneously</em>, by a restriction on the
 *          associated entity table. The {@link Where @Where} annotation always
 *          filters entity tables.
 *
 * @author Emmanuel Bernard
 *
 * @see Where
 *
 * @deprecated Use {@link SQLJoinTableRestriction}
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Deprecated(since = "6.3")
public @interface WhereJoinTable {
	/**
	 * A predicate, written in native SQL.
	 */
	String clause();
}
