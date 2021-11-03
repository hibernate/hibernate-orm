/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.contribution.jts.infrastructure;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class CustomJdbcServicesInitiator implements StandardServiceInitiator<JdbcServices> {
	@Override
	public JdbcServices initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final JdbcServices standard = JdbcServicesInitiator.INSTANCE.initiateService( configurationValues, registry );
		return new CustomJdbcServices( standard );
	}

	@Override
	public Class<JdbcServices> getServiceInitiated() {
		return JdbcServices.class;
	}
}
