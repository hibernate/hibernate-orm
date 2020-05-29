/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.spi.SessionFactoryBuilderService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class DefaultSessionFactoryBuilderInitiator implements StandardServiceInitiator<SessionFactoryBuilderService> {

	public static final DefaultSessionFactoryBuilderInitiator INSTANCE = new DefaultSessionFactoryBuilderInitiator();

	private DefaultSessionFactoryBuilderInitiator() {
	}

	@Override
	public SessionFactoryBuilderService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return DefaultSessionFactoryBuilderService.INSTANCE;
	}

	@Override
	public Class<SessionFactoryBuilderService> getServiceInitiated() {
		return SessionFactoryBuilderService.class;
	}

}
