package org.hibernate.engine.spi;

/**
 * The type of action from which the cache call is originating.
 */
public enum CachedNaturalIdValueSource {
	LOAD,
	INSERT,
	UPDATE
}
