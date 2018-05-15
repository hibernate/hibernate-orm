/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.service.spi.ServiceException;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class StrategyCreatorRegionFactoryImpl<R extends RegionFactory> implements Function<Class<R>,R> {
	private static final Logger log = Logger.getLogger( StrategyCreatorRegionFactoryImpl.class );

	private final Properties properties;

	public StrategyCreatorRegionFactoryImpl(Properties properties) {
		this.properties = properties;
	}

	@Override
	public R apply(Class<R> strategyClass) {
		assert RegionFactory.class.isAssignableFrom( strategyClass );

		// first look for a constructor accepting Properties
		try {
			final Constructor<R> ctor = strategyClass.getConstructor( Properties.class );
			return ctor.newInstance( properties );
		}
		catch ( NoSuchMethodException e ) {
			log.debugf( "RegionFactory impl [%s] did not provide constructor accepting Properties", strategyClass.getName() );
		}
		catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			throw new ServiceException( "Unable to call constructor of RegionFactory impl [" + strategyClass.getName() + "]", e );
		}

		// next try Map
		try {
			final Constructor<R> ctor = strategyClass.getConstructor( Map.class );
			return ctor.newInstance( properties );
		}
		catch ( NoSuchMethodException e ) {
			log.debugf( "RegionFactory impl [%s] did not provide constructor accepting Properties", strategyClass.getName() );
		}
		catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			throw new ServiceException( "Unable to call constructor of RegionFactory impl [" + strategyClass.getName() + "]", e );
		}

		// finally try no-arg
		try {
			return strategyClass.newInstance();
		}
		catch (IllegalAccessException | InstantiationException e) {
			throw new ServiceException( "Unable to call constructor of RegionFactory impl [" + strategyClass.getName() + "]", e );
		}
	}
}
