/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.testsupport;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @author Steve Ebersole
 */
public final class CdiContainerLinker {
	public static class StandardResolver implements ServiceRegistry.SettingResolver {
		@Override
		public Object resolve(StandardServiceRegistryBuilder registryBuilder, ExtensionContext junitContext) {
			final CdiContainerScope containerScope = CdiContainerExtension.findCdiContainerScope(
					junitContext.getRequiredTestInstance(),
					junitContext
			);
			return containerScope.getBeanManager();
		}
	}

	public static class ExtendedResolver implements ServiceRegistry.SettingResolver {
		@Override
		public Object resolve(StandardServiceRegistryBuilder registryBuilder, ExtensionContext junitContext) {
			final CdiContainerScope containerScope = CdiContainerExtension.findCdiContainerScope(
					junitContext.getRequiredTestInstance(),
					junitContext
			);
			return containerScope.getExtendedBeanManager();
		}
	}
}
