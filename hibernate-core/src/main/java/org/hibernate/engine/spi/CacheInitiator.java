/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public Class<CacheImplementor> getServiceInitiated() {
		return CacheImplementor.class;
	}

	@Override
	public CacheImplementor initiateService(SessionFactoryServiceInitiatorContext context) {
		final RegionFactory regionFactory = context.getServiceRegistry().getService( RegionFactory.class );
		return ( !NoCachingRegionFactory.class.isInstance( regionFactory ) )
				? new EnabledCaching( context.getSessionFactory() )
				: new DisabledCaching( context.getSessionFactory() );
	}
}
