/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;


import jakarta.persistence.EntityGraph;
import jakarta.persistence.FindOption;

import java.util.List;

/**
 * Indicates whether the result list should be ordered relative to the
 * position of the identifier list.  E.g.
 * <pre>
 * List&lt;Person&gt; results = session.findMultiple(
 *     Person.class,
 *     List.of(1,2,3,2),
 *     ORDERED
 * );
 * assert results.get(0).getId() == 1;
 * assert results.get(1).getId() == 2;
 * assert results.get(2).getId() == 3;
 * assert results.get(3).getId() == 2;
 * </pre>
 * <p>
 * The default is {@link #ORDERED}.
 *
 * @see org.hibernate.Session#findMultiple(Class, List, FindOption...)
 * @see org.hibernate.Session#findMultiple(EntityGraph, List , FindOption...)
 *
 * @since 7.2
 */
public enum OrderingMode implements FindMultipleOption {
	/**
	 * The default.  The result list is ordered relative to the
	 * position of the identifiers list.
	 *
	 * @see RemovalsMode
	 */
	ORDERED,
	/**
	 * The result list may be in any order.
	 */
	UNORDERED
}
