/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.dialect.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard initiator for the standard {@link DialectFactory} service
 *
 * @author Steve Ebersole
 */
public class DialectFactoryInitiator implements StandardServiceInitiator<DialectFactory> {
	/**
	 * Singleton access
	 */
	public static final DialectFactoryInitiator INSTANCE = new DialectFactoryInitiator();

	@Override
	public Class<DialectFactory> getServiceInitiated() {
		return DialectFactory.class;
	}

	@Override
	public DialectFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new DialectFactoryImpl();
	}
}
