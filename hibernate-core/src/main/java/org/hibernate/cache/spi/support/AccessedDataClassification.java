/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

/**
 * @author Steve Ebersole
 */
public enum AccessedDataClassification {
	ENTITY,
	NATURAL_ID,
	COLLECTION,
	QUERY_RESULTS,
	TIMESTAMPS
}
