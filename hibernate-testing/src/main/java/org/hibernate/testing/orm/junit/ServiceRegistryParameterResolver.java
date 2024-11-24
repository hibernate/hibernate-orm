/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.registry.StandardServiceRegistry;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * @author Steve Ebersole
 */
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
