/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that mappings from the natural id values of the annotated
 * entity to the corresponding entity id values should be cached in the
 * shared second-level cache. This allows Hibernate to sometimes avoid
 * round trip to the database when a cached entity is retrieved by its
 * natural id.
 * <p>
 * This annotation is usually used in combination with {@link Cache},
 * since a round trip may only be avoided if the entity itself is
 * also available in the cache.
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
