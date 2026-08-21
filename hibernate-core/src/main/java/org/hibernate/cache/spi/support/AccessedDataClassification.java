/*
 * SPDX-License-Identifier: Apache-2.0
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
