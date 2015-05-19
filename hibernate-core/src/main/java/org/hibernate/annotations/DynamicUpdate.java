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
 * For updating, should this entity use dynamic sql generation where only changed columns get referenced in the
 * prepared sql statement?
 * <p/>
 * Note, for re-attachment of detached entities this is not possible without select-before-update being enabled.
 *
 * @author Steve Ebersole
 *
 * @see SelectBeforeUpdate
 */
@Target( TYPE )
@Retention( RUNTIME )
public @interface DynamicUpdate {
	/**
	 * Should dynamic update generation be used for this entity?  {@code true} says the update sql will be dynamic
	 * generated.  Default is {@code true} (since generally this annotation is not used unless the user wants dynamic
	 * generation).
	 */
	boolean value() default true;
}
