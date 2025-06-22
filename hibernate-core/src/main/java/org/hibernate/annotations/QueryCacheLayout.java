/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Configures the layout for the entity or collection data in a query cache.
 *
 * @see CacheLayout
 * @see jakarta.persistence.Cacheable
 * @see org.hibernate.Cache
 * @see org.hibernate.cfg.AvailableSettings#CACHE_REGION_FACTORY
 * @see org.hibernate.cfg.AvailableSettings#USE_SECOND_LEVEL_CACHE
 * @see org.hibernate.cfg.AvailableSettings#USE_QUERY_CACHE
 * @since 6.5
 *
 */
@Target({TYPE, FIELD, METHOD})
@Retention(RUNTIME)
@Incubating
public @interface QueryCacheLayout {

	/**
	 * The layout of the data for an entity or collection in the query cache.
	 */
	CacheLayout layout() default CacheLayout.AUTO;
}
