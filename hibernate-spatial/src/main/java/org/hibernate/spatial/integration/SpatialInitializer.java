/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.integration;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Manages initialization of the hibernate-spatial integration
 *
 * @author Karel Maesen, Geovise BVBA
 * @author Steve Ebersole
 */
public class SpatialInitializer implements ServiceContributor {

	@Override
	public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		final SpatialService spatialService = new SpatialService( serviceRegistryBuilder );
		serviceRegistryBuilder.addService( SpatialService.class, spatialService );

		if ( !spatialService.isEnabled() ) {
			return;
		}

		serviceRegistryBuilder.addInitiator( SpatialDialectFactoryInitiator.INSTANCE );
	}

}
