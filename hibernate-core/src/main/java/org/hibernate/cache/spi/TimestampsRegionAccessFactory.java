/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

/**
 * Responsible for building the TimestampsRegionAccessFactory to use for
 * managing query results in regards to staleness
 *
 * @author Steve Ebersole
 */
public interface TimestampsRegionAccessFactory {
	TimestampsRegionAccess buildTimestampsRegionAccess(CacheImplementor cacheManager, TimestampsRegion timestampsRegion);
}
