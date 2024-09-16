/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.hint;

/**
 * Contract defining how query hints get applied.
 *
 * @author Vlad Mihalcea
 */
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
