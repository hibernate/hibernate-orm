/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.extension.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.Incubating;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.extension.internal.ExtensionIntegrationServiceImpl;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;

import java.util.Set;

@Incubating
public class ExtensionIntegrationServiceInitiator
		implements SessionFactoryServiceInitiator<ExtensionIntegrationService> {

	public static final ExtensionIntegrationServiceInitiator INSTANCE = new ExtensionIntegrationServiceInitiator();

	@Override
	@Nonnull
	public ExtensionIntegrationService initiateService(@Nonnull SessionFactoryServiceInitiatorContext context) {
		final var classLoaderService = context.getServiceRegistry().requireService( ClassLoaderService.class );
		return ExtensionIntegrationServiceImpl.create( Set.of(), classLoaderService );
	}

	@Override
	@Nonnull
	public Class<ExtensionIntegrationService> getServiceInitiated() {
		return ExtensionIntegrationService.class;
	}
}
