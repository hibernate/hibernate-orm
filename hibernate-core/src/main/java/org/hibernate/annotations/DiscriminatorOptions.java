/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
// $Id$
package org.hibernate.annotations;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Optional annotation to express Hibernate specific discriminator properties.
 *
 * @author Hardy Ferentschik
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface DiscriminatorOptions {
	/**
	 * "Forces" Hibernate to specify the allowed discriminator values, even when retrieving all instances of
	 * the root class.  {@code true} indicates that the discriminator value should be forced; Default is
	 * {@code false}.
	 */
	boolean force() default false;

	/**
	 * Set this to {@code false} if your discriminator column is also part of a mapped composite identifier.
	 * It tells Hibernate not to include the column in SQL INSERTs.  Default is {@code true}.
	 */
	boolean insert() default true;
}
