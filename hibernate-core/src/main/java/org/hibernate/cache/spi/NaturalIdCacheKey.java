/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

/**
 * Defines a key for caching natural identifier resolutions into the second level cache.
 *
 * @author Sanne Grinovero
 * @since 5.0
 */
public interface NaturalIdCacheKey extends CacheKey {

	String getEntityName();

	Object[] getNaturalIdValues();

	String getTenantId();

}
