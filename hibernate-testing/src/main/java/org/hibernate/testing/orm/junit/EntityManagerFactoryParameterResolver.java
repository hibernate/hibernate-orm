/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import javax.persistence.EntityManagerFactory;

import static org.hibernate.testing.orm.junit.EntityManagerFactoryExtension.findEntityManagerFactoryScope;

/**
 * @author Steve Ebersole
 */
public class EntityManagerFactoryParameterResolver implements ParameterResolver {
	@Override
	public boolean supportsParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		return JUnitHelper.supportsParameterInjection(
				parameterContext,
				EntityManagerFactory.class,
				EntityManagerFactoryScope.class
		);
	}

	@Override
	public Object resolveParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		final EntityManagerFactoryScope scope = findEntityManagerFactoryScope(
				extensionContext.getRequiredTestInstance(),
				extensionContext
		);

		if ( EntityManagerFactoryScope.class.isAssignableFrom( parameterContext.getParameter().getType() ) ) {
			return scope;
		}

		assert EntityManagerFactory.class.isAssignableFrom( parameterContext.getParameter().getType() );
		return scope.getEntityManagerFactory();
	}
}
