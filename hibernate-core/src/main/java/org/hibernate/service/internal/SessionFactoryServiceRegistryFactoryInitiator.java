/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryServiceRegistryFactoryInitiator implements StandardServiceInitiator<SessionFactoryServiceRegistryFactory> {
	public static final SessionFactoryServiceRegistryFactoryInitiator INSTANCE = new SessionFactoryServiceRegistryFactoryInitiator();

	@Override
	public Class<SessionFactoryServiceRegistryFactory> getServiceInitiated() {
		return SessionFactoryServiceRegistryFactory.class;
	}

	@Override
	public SessionFactoryServiceRegistryFactoryImpl initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new SessionFactoryServiceRegistryFactoryImpl( registry );
	}
}
