/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
