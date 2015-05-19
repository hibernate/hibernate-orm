/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.integration;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.spatial.HibernateSpatialConfiguration;

import org.geolatte.geom.codec.db.oracle.ConnectionFinder;

import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;
import static org.hibernate.spatial.HibernateSpatialConfiguration.AvailableSettings.CONNECTION_FINDER;
import static org.hibernate.spatial.HibernateSpatialConfiguration.AvailableSettings.OGC_STRICT;

/**
 * The {@code StandardServiceInitiator} for Hibernate Spatial's specialized DialectFactory.
 *
 * @author Karel Maesen, Geovise BVBA
 * @author Steve Ebersole
 */
public class SpatialDialectFactoryInitiator implements StandardServiceInitiator<DialectFactory> {
	/**
	 * Singleton access
	 */
	public static final SpatialDialectFactoryInitiator INSTANCE = new SpatialDialectFactoryInitiator();

	@Override
	public Class<DialectFactory> getServiceInitiated() {
		return DialectFactory.class;
	}

	@Override
	public SpatialDialectFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final ConfigurationService cfgService = registry.getService( ConfigurationService.class );
		final StrategySelector strategySelector = registry.getService( StrategySelector.class );

		final HibernateSpatialConfiguration spatialConfig = new HibernateSpatialConfiguration(
				cfgService.getSetting( OGC_STRICT, BOOLEAN, null ),
				strategySelector.resolveStrategy(
						ConnectionFinder.class,
						cfgService.getSettings().get( CONNECTION_FINDER )
				)
		);

		return new SpatialDialectFactory( spatialConfig );
	}
}
