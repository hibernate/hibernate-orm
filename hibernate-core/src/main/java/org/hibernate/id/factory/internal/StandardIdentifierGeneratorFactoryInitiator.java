/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.factory.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class StandardIdentifierGeneratorFactoryInitiator implements StandardServiceInitiator<IdentifierGeneratorFactory> {
	public static final StandardIdentifierGeneratorFactoryInitiator INSTANCE = new StandardIdentifierGeneratorFactoryInitiator();

	@Override
	public Class<IdentifierGeneratorFactory> getServiceInitiated() {
		return IdentifierGeneratorFactory.class;
	}

	@Override
	public IdentifierGeneratorFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new StandardIdentifierGeneratorFactory( registry );
	}
}
