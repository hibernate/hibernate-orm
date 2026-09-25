package org.hibernate.cache.spi.support;

/**
 * @author Steve Ebersole
 *
 * @deprecated No longer used
 */
@Deprecated(since = "7.4")
public enum AccessedDataClassification {
	ENTITY,
	NATURAL_ID,
	COLLECTION,
	QUERY_RESULTS,
	TIMESTAMPS
}
