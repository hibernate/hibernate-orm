/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jndi.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard initiator for the standard {@link JndiService} service
 *
 * @author Steve Ebersole
 */
public class JndiServiceInitiator implements StandardServiceInitiator<JndiService> {
	/**
	 * Singleton access
	 */
	public static final JndiServiceInitiator INSTANCE = new JndiServiceInitiator();

	@Override
	public Class<JndiService> getServiceInitiated() {
		return JndiService.class;
	}

	@Override
	public JndiService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new JndiServiceImpl( configurationValues );
	}
}
