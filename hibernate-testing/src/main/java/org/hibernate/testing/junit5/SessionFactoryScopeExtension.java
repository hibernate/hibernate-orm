/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.junit5;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;

/**
 * The thing that actually manages lifecycle of the SessionFactory related to a
 * test class.  Work in conjunction with SessionFactoryScope and SessionFactoryScopeContainer
 *
 * @see SessionFactoryScope
 * @see SessionFactoryScopeContainer
 * @see SessionFactoryProducer
 *
 * @author Steve Ebersole
 */
public class SessionFactoryScopeExtension
		implements TestInstancePostProcessor, AfterAllCallback, TestExecutionExceptionHandler {

	public static final ExtensionContext.Namespace NAMESPACE = create( SessionFactoryScopeExtension.class.getName() );

	public SessionFactoryScopeExtension() {
		System.out.println( "SessionFactoryScopeExtension#<init>" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TestInstancePostProcessor

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
		System.out.println( "SessionFactoryScopeExtension#postProcessTestInstance" );

		if ( SessionFactoryScopeContainer.class.isInstance( testInstance ) ) {
			final SessionFactoryScopeContainer scopeContainer = SessionFactoryScopeContainer.class.cast(
					testInstance );
			final SessionFactoryScope scope = new SessionFactoryScope( scopeContainer.getSessionFactoryProducer() );
			context.getStore( NAMESPACE ).put( testInstance, scope );

			scopeContainer.injectSessionFactoryScope( scope );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AfterAllCallback

	@Override
	public void afterAll(ExtensionContext context) {
		final SessionFactoryScope scope = (SessionFactoryScope) context.getStore( NAMESPACE )
				.remove( context.getRequiredTestInstance() );
		if ( scope != null ) {
			scope.releaseSessionFactory();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TestExecutionExceptionHandler

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		final SessionFactoryScope scope = (SessionFactoryScope) context.getStore( NAMESPACE )
				.get( context.getRequiredTestInstance() );
		if ( scope != null ) {
			scope.releaseSessionFactory();
		}

		throw throwable;
	}
}
