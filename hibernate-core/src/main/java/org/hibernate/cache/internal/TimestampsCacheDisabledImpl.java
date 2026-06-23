/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * TimestampsRegionAccess implementation for cases where query results caching
 * (or second level caching overall) is disabled.
 *
 * @author Steve Ebersole
 */
public class TimestampsCacheDisabledImpl implements TimestampsCache {
	@Override
	@Nullable
	public TimestampsRegion getRegion() {
		return null;
	}

	@Override
	public void preInvalidate(@Nonnull String[] spaces, @Nonnull SharedSessionContractImplementor session) {
		//noop
	}

	@Override
	public void invalidate(@Nonnull String[] spaces, @Nonnull SharedSessionContractImplementor session) {
		//noop
	}

	@Override
	public boolean isUpToDate(
			@Nonnull String[] spaces,
			@Nonnull Long timestamp,
			@Nonnull SharedSessionContractImplementor session) {
		//noop
		return false;
	}

	@Override
	public boolean isUpToDate(
			@Nonnull Collection<String> spaces,
			@Nonnull Long timestamp,
			@Nonnull SharedSessionContractImplementor session) {
		//noop
		return false;
	}

	@Override
	public void clear() throws CacheException {
	}
}
