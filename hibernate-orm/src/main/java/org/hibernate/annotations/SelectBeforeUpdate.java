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
 * Should the entity's current state be selected from the database when determining whether to perform an update when
 * re-attaching detached entities?
 *
 * @author Steve Ebersole
 */
@Target( TYPE )
@Retention( RUNTIME )
public @interface SelectBeforeUpdate {
	/**
	 * {@code true} (which is the default when this annotation is present) indicates that
	 * {@code select-beforeQuery-update} processing should occur.  {@code false} indicates
	 * {@code select-beforeQuery-update} processing should not occur.
	 */
	boolean value() default true;
}
