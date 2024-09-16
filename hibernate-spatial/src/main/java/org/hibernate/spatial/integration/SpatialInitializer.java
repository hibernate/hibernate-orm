/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
	}

}
