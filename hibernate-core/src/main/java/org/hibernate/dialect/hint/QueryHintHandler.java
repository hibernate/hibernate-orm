/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.hint;

/**
 * @deprecated Moved to {@link org.hibernate.dialect.Dialect}
 */
@Deprecated(since = "7.0", forRemoval = true)
public interface QueryHintHandler {

	/**
	 * Add query hints to the given query.
	 *
	 * @param query original query
	 * @param hints hints to be applied
	 * @return query with hints
	 */
	String addQueryHints(String query, String hints);
}
