/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.jboss.logging.Logger;

/**
 * TimestampsRegionAccess implementation for cases where query results caching
 * (or second level caching overall) is disabled.
 *
 * @author Steve Ebersole
 */
public class TimestampsCacheDisabledImpl implements TimestampsCache {
	private static final Logger log = Logger.getLogger( TimestampsCacheDisabledImpl.class );

	@Override
	public TimestampsRegion getRegion() {
		return null;
	}

	@Override
	public void preInvalidate(String[] spaces, SharedSessionContractImplementor session) {
		log.trace( "TimestampsRegionAccess#preInvalidate - disabled" );
	}

	@Override
	public void invalidate(String[] spaces, SharedSessionContractImplementor session) {
		log.trace( "TimestampsRegionAccess#invalidate - disabled" );
	}

	@Override
	public boolean isUpToDate(
			String[] spaces,
			Long timestamp,
			SharedSessionContractImplementor session) {
		log.trace( "TimestampsRegionAccess#isUpToDate - disabled" );
		return false;
	}
}
