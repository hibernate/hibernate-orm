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
 * For inserting, should this entity use dynamic sql generation where only non-null columns get referenced in the 
 * prepared sql statement?
 *
 * @author Steve Ebersole
 */
@Target( TYPE )
@Retention( RUNTIME )
public @interface DynamicInsert {
	/**
	 * Should dynamic insertion be used for this entity?  {@code true} says dynamic insertion will be used.
	 * Default is {@code true} (since generally this annotation is not used unless the user wants dynamic insertion).
	 */
	boolean value() default true;
}
