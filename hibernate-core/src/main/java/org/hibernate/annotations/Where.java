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
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a restriction written in native SQL to add to the generated
 * SQL when querying an entity or collection.
 * <p>
 * For example, {@code @Where("deleted = false")} could be used to hide
 * entity instances which have been soft-deleted.
 * <p>
 * Note that {@code Where} restrictions are always applied and cannot be
 * disabled. They're therefore much less flexible than {@link Filter filters}.
 *
 * @see Filter
 *
 * @author Emmanuel Bernard
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Where {
	/**
	 * A predicate, written in native SQL.
	 */
	String clause();
}
