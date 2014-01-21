package org.hibernate.spatial.integration;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/21/14
 */
public class SpatialDialectFactoryContributor implements ServiceContributor {

	@Override
	public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.addInitiator( new SpatialInitiator() );
	}

}
