package org.hibernate.annotations;

/**
 * Cascade types (can override default EJB3 cascades
 */
public enum CascadeType {
	ALL,
	PERSIST,
	MERGE,
	REMOVE,
	REFRESH,
	DELETE,
	SAVE_UPDATE,
	REPLICATE,
	DELETE_ORPHAN,
	LOCK,
	EVICT
}
