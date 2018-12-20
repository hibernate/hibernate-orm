/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm;

import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryExtension
		implements TestInstancePostProcessor, AfterAllCallback, TestExecutionExceptionHandler {

	private static final String SESSION_FACTORY_KEY = SessionFactoryScope.class.getName();

	private static ExtensionContext.Store locateExtensionStore(Object testInstance, ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( SessionFactoryExtension.class, context, testInstance );
	}

	public static Optional<SessionFactoryScope> findSessionFactoryScope(Object testInstance, ExtensionContext context) {
		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		return Optional.ofNullable( (SessionFactoryScope) store.get( SESSION_FACTORY_KEY ) );
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
		if ( !context.getElement().isPresent() ) {
			throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
		}

		final SessionFactoryScopeImpl sfScope = new SessionFactoryScopeImpl(
				TestDomainExtension.findMetamodel( testInstance, context ).orElseThrow(
						() -> new IllegalStateException( "Expecting to find Metamodel" )
				),
				AnnotationSupport.findAnnotation( context.getElement().get(), SessionFactory.class ).orElseThrow(
						() -> new IllegalStateException( "Expecting to find @SessionFactory" )
				)
		);

		locateExtensionStore( testInstance, context ).put( SESSION_FACTORY_KEY, sfScope );

		if ( testInstance instanceof SessionFactoryScopeAware ) {
			( (SessionFactoryScopeAware) testInstance ).injectSessionFactoryScope( sfScope );
		}
	}

	@Override
	public void afterAll(ExtensionContext context) {
		final Object testInstance = context.getRequiredTestInstance();

		if ( testInstance instanceof SessionFactoryScopeAware ) {
			( (SessionFactoryScopeAware) testInstance ).injectSessionFactoryScope( null );
		}

		final SessionFactoryScopeImpl removed = (SessionFactoryScopeImpl) locateExtensionStore( testInstance, context ).remove( SESSION_FACTORY_KEY );
		if ( removed != null ) {
			removed.release();
		}
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		final Object testInstance = context.getRequiredTestInstance();
		final SessionFactoryScopeImpl scope = (SessionFactoryScopeImpl) locateExtensionStore( testInstance, context ).get( SESSION_FACTORY_KEY );
		scope.release();
	}
}
