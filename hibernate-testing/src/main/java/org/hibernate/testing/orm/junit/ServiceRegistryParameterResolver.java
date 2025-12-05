/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.registry.StandardServiceRegistry;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/// ParameterResolver for [ServiceRegistryExtension], capable of resolving
/// either [ServiceRegistryScope] or [StandardServiceRegistry] references.
///
/// @author Steve Ebersole
public class ServiceRegistryParameterResolver implements ParameterResolver {
	@Override
	public boolean supportsParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		return JUnitHelper.supportsParameterInjection(
				parameterContext,
				StandardServiceRegistry.class,
				ServiceRegistryScope.class
		);
	}

	@Override
	public Object resolveParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		final ServiceRegistryScope scope = ServiceRegistryExtension.findServiceRegistryScope(
				extensionContext.getRequiredTestInstance(),
				extensionContext
		);

		final Class<?> paramType = parameterContext.getParameter().getType();
		if ( paramType.isAssignableFrom( ServiceRegistryScope.class ) ) {
			return scope;
		}
		else if ( paramType.isAssignableFrom( StandardServiceRegistry.class ) ) {
			return scope.getRegistry();
		}

		throw new IllegalStateException(
				"Unexpected parameter type [" + paramType.getName() + "] for service-registry injection"
		);
	}
}
