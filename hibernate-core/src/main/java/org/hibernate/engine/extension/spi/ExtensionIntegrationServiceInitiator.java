/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.extension.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.extension.internal.ExtensionIntegrationServiceImpl;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;

import java.util.Set;

@Incubating
public class ExtensionIntegrationServiceInitiator
		implements SessionFactoryServiceInitiator<ExtensionIntegrationService> {

	public static final ExtensionIntegrationServiceInitiator INSTANCE = new ExtensionIntegrationServiceInitiator();

	@Override
	public ExtensionIntegrationService initiateService(SessionFactoryServiceInitiatorContext context) {
		return ExtensionIntegrationServiceImpl.create( Set.of(), context.getSessionFactory().getClassLoaderService() );
	}

	@Override
	public Class<ExtensionIntegrationService> getServiceInitiated() {
		return ExtensionIntegrationService.class;
	}
}
