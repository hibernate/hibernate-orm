/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm;

import org.hibernate.engine.spi.SessionFactoryImplementor;

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

	private static final String SESSION_FACTORY_KEY = SessionFactoryImplementor.class.getName();

	private static ExtensionContext.Store locateExtensionStore(Object testInstance, ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( ServiceRegistryExtension.class, context, testInstance );
	}

	@Override
	@SuppressWarnings("RedundantClassCall")
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
		if ( !context.getElement().isPresent() ) {
			throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
		}

		if ( ! SessionFactoryScopeAware.class.isInstance( testInstance ) ) {
			throw new RuntimeException(
					"Test instance [" + testInstance + "] does not implement `" +
							SessionFactoryScopeAware.class.getName() + "`"
			);
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

		( (SessionFactoryScopeAware) testInstance ).injectSessionFactoryScope( sfScope );
	}

	@Override
	public void afterAll(ExtensionContext context) {
		( (SessionFactoryScopeAware) context.getRequiredTestInstance() ).injectSessionFactoryScope( null );
		final SessionFactoryScopeImpl removed = (SessionFactoryScopeImpl) locateExtensionStore( context.getRequiredTestInstance(), context ).remove( SESSION_FACTORY_KEY );
		if ( removed != null ) {
			removed.release();
		}
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {

	}
}
