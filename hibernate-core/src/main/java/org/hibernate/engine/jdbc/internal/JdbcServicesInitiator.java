/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard initiator for the standard {@link JdbcServices} service
 *
 * TODO : should this maybe be a SessionFactory service?
 *
 * @author Steve Ebersole
 */
public class JdbcServicesInitiator implements StandardServiceInitiator<JdbcServices> {
	/**
	 * Singleton access
	 */
	public static final JdbcServicesInitiator INSTANCE = new JdbcServicesInitiator();

	@Override
	public Class<JdbcServices> getServiceInitiated() {
		return JdbcServices.class;
	}

	@Override
	public JdbcServices initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new JdbcServicesImpl();
	}
}
