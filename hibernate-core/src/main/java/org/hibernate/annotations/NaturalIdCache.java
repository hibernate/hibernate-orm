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
 * Specifies that the natural id values associated with the annotated
 * entity should be cached in the shared second-level cache.
 *
 * @author Eric Dalquist
 * @author Steve Ebersole
 *
 * @see NaturalId
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface NaturalIdCache {
	/**
	 * Specifies an explicit cache region name.
	 * <p>
	 * By default, the region name is {@code EntityName##NaturalId}.
	 */
	String region() default "";
}
