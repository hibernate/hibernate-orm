/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa;

import org.hibernate.query.Query;

/**
 * @author Steve Ebersole
 */
public enum CachingType {
	/**
	 * No caching is done
	 */
	NONE,
	/**
	 * Caching is done related to entity, collection and natural-id data
	 */
	DATA,
	/**
	 * Caching is done on the results of {@linkplain Query} executions
	 */
	QUERY,
	/**
	 * Both {@linkplain #DATA} and {@linkplain #QUERY} are enabled.
	 */
	BOTH,
	/**
	 * Implicit setting.  Defaults to {@linkplain #NONE}
	 */
	AUTO
}
