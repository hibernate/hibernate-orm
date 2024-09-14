/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Explicitly specifies the target entity type in an association,
 * avoiding reflection and generics resolution. This annotation is
 * almost never useful.
 *
 * @author Emmanuel Bernard
 *
 * @deprecated use annotation members of JPA association mapping
 *             annotations, for example,
 *             {@link jakarta.persistence.OneToMany#targetEntity()}
 */
@Deprecated(since = "6.2")
@java.lang.annotation.Target({FIELD, METHOD})
@Retention(RUNTIME)
public @interface Target {
	/**
	 * The target entity type.
	 */
	Class<?> value();
}
