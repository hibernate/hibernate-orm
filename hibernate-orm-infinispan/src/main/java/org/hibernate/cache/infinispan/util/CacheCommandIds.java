/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

/**
 * Command id range assigned to Hibernate second level cache: 120 - 139
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
public interface CacheCommandIds {
	/**
	 * {@link EvictAllCommand} id
	 */
	byte EVICT_ALL = 120;

	/**
	 * {@link EndInvalidationCommand} id
	 */
	byte END_INVALIDATION = 121;

	/**
	 * {@link BeginInvalidationCommand} id
	 */
	byte BEGIN_INVALIDATION = 122;
}
