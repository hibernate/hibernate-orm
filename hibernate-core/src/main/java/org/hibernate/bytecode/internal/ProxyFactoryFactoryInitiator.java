/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class ProxyFactoryFactoryInitiator implements StandardServiceInitiator<ProxyFactoryFactory> {

	/**
	 * Singleton access
	 */
	public static final StandardServiceInitiator<ProxyFactoryFactory> INSTANCE = new ProxyFactoryFactoryInitiator();

	@Override
	public ProxyFactoryFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return Environment.getBytecodeProvider().getProxyFactoryFactory();
	}

	@Override
	public Class<ProxyFactoryFactory> getServiceInitiated() {
		return ProxyFactoryFactory.class;
	}
}
