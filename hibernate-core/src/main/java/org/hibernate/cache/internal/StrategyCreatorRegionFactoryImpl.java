/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.registry.selector.spi.StrategyCreator;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.service.spi.ServiceException;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class StrategyCreatorRegionFactoryImpl implements StrategyCreator<RegionFactory> {
	private static final Logger log = Logger.getLogger( StrategyCreatorRegionFactoryImpl.class );

	private final Properties properties;

	public StrategyCreatorRegionFactoryImpl(Properties properties) {
		this.properties = properties;
	}

	@Override
	public RegionFactory create(Class<? extends RegionFactory> strategyClass) {
		assert RegionFactory.class.isAssignableFrom( strategyClass );

		// first look for a constructor accepting Properties
		final RegionFactory regionFactoryWithProperties = instantiateWithProperties( strategyClass, Properties.class );
		if ( regionFactoryWithProperties != null ) {
			return regionFactoryWithProperties;
		}
		// next try Map
		final RegionFactory regionFactoryWithMap = instantiateWithProperties( strategyClass, Map.class );
		if ( regionFactoryWithMap != null ) {
			return regionFactoryWithMap;
		}
		// finally try no-arg
		try {
			return strategyClass.newInstance();
		}
		catch (IllegalAccessException | InstantiationException e) {
			throw new ServiceException( "Unable to call constructor of RegionFactory impl [" + strategyClass.getName() + "]", e );
		}
	}

	private RegionFactory instantiateWithProperties(Class<? extends RegionFactory> strategyClass, Class<?> propertiesClass) {
		try {
			return strategyClass.getConstructor( propertiesClass ).newInstance( properties );
		}
		catch ( NoSuchMethodException e ) {
			log.debugf( "RegionFactory impl [%s] did not provide constructor accepting Properties", strategyClass.getName() );
			return null;
		}
		catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			throw new ServiceException( "Unable to call constructor of RegionFactory impl [" + strategyClass.getName() + "]", e );
		}
	}
}
