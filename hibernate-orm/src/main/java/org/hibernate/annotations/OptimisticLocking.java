/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to define the style of optimistic locking to be applied to an entity.  In a hierarchy, only valid on the
 * root entity.
 *
 * @author Steve Ebersole
 */
@Target( TYPE )
@Retention( RUNTIME )
public @interface OptimisticLocking {
	/**
	 * Defines the style of optimistic locking for the entity.
	 */
	OptimisticLockType type() default OptimisticLockType.VERSION;
}
