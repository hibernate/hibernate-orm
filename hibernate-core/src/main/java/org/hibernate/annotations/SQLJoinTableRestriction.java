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
 * Specifies a restriction written in native SQL to add to the generated SQL
 * when querying the {@linkplain jakarta.persistence.JoinTable join table}
 * of a collection.
 * <p>
 * For example, <code>&#64;SQLJoinTableRestriction("status &lt;&gt; 'DELETED'")</code>
 * could be used to hide associations which have been soft-deleted from an
 * association table.
 *
 * @apiNote This separate annotation is useful because it's possible to filter
 *          a many-to-many association <em>both</em> by a restriction on the
 *          join table, and, <em>simultaneously</em>, by a restriction on the
 *          associated entity table. The {@link SQLRestriction @SQLRestriction}
 *          annotation always filters entity tables.
 *
 * @since 6.3
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 *
 * @see SQLRestriction
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface SQLJoinTableRestriction {
	/**
	 * A predicate, written in native SQL.
	 */
	String value();
}
