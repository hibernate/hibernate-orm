/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import java.util.Collection;

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

	@Override
	public boolean isUpToDate(
			Collection<String> spaces,
			Long timestamp,
			SharedSessionContractImplementor session) {
		log.trace( "TimestampsRegionAccess#isUpToDate - disabled" );
		return false;
	}
}
