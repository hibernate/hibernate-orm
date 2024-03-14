/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.Optional;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

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

		// Fall back on the test class annotation in case the method isn't annotated or we're in a @Before/@After method
		Optional<Jpa> emfAnnWrapper = AnnotationSupport.findAnnotation(
				extensionContext.getRequiredTestClass(),
				Jpa.class
		);
		Object testScope = extensionContext.getRequiredTestInstance();

		// coming from a @Test
		if (parameterContext.getDeclaringExecutable() instanceof java.lang.reflect.Method && !extensionContext.getTestMethod().isEmpty()) {

			Optional<Jpa> testEmfAnnWrapper = AnnotationSupport.findAnnotation(
					extensionContext.getRequiredTestMethod(),
					Jpa.class
			);
			// @Jpa on the test, so override the class annotation
			if ( !testEmfAnnWrapper.isEmpty() ) {
				testScope = extensionContext.getRequiredTestMethod();
				emfAnnWrapper = testEmfAnnWrapper;
			}
		}

		final EntityManagerFactoryScope scope = findEntityManagerFactoryScope(
				testScope,
				emfAnnWrapper,
				extensionContext
		);

		if ( EntityManagerFactoryScope.class.isAssignableFrom( parameterContext.getParameter().getType() ) ) {
			return scope;
		}

		assert EntityManagerFactory.class.isAssignableFrom( parameterContext.getParameter().getType() );
		return scope.getEntityManagerFactory();
	}
}
