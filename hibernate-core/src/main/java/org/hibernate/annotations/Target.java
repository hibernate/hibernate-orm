/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
@Deprecated
@java.lang.annotation.Target({ElementType.FIELD, ElementType.METHOD})
@Retention( RetentionPolicy.RUNTIME )
public @interface Target {
	/**
	 * The target entity type.
	 */
	Class<?> value();
}
