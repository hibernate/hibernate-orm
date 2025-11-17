/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.cache.internal.DisabledCaching;
import org.hibernate.cache.internal.EnabledCaching;
import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;

/**
 * Initiator for second level cache support
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public class CacheInitiator implements SessionFactoryServiceInitiator<CacheImplementor> {
	public static final CacheInitiator INSTANCE = new CacheInitiator();

	@Override
	public CacheImplementor initiateService(SessionFactoryServiceInitiatorContext context) {
		final var regionFactory = context.getServiceRegistry().getService( RegionFactory.class );
		return regionFactory instanceof NoCachingRegionFactory
				? new DisabledCaching( context.getSessionFactory() )
				: new EnabledCaching( context.getSessionFactory() );
	}

	@Override
	public Class<CacheImplementor> getServiceInitiated() {
		return CacheImplementor.class;
	}
}
