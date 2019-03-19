package org.hibernate.cache.internal;

public class CacheKeyHelper {

	/**
	 * Private constructor, this Class cannot be instantiated. Static usage
	 */
	private CacheKeyHelper() {
	}

	/**
	 * @param key
	 *            with expected type {@link}CacheKeyImplementation
	 * @return Long id off the key
	 */
	public static Object getId(Object key) {
		return ((CacheKeyImplementation) key).getId();
	}
}
