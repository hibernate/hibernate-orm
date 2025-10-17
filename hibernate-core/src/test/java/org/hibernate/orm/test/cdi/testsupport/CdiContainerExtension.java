/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.testsupport;

import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.hibernate.testing.orm.junit.JUnitHelper;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Optional;

/**
 * @author Steve Ebersole
 */
public class CdiContainerExtension implements TestInstancePostProcessor, BeforeEachCallback {
	private static final String CONTEXT_KEY = CdiContainerScope.class.getName();

	/**
	 * Intended for use from external consumers.  Will never create a scope, just
	 * attempt to consume an already created and stored one
	 */
	public static CdiContainerScope findCdiContainerScope(Object testInstance, ExtensionContext context) {
		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		final CdiContainerScope existing = (CdiContainerScope) store.get( CONTEXT_KEY );
		if ( existing != null ) {
			return existing;
		}

		throw new RuntimeException( "Could not locate CdiContainerScope : " + context.getDisplayName() );
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
		final Optional<CdiContainer> annRef = AnnotationSupport.findAnnotation(
				context.getRequiredTestClass(),
				CdiContainer.class
		);

		if( annRef.isPresent() ) {
			final ExtensionContext.Store extensionStore = locateExtensionStore( testInstance, context );
			var scope = new CdiContainerScope( () -> {
				final SeContainerInitializer initializer = SeContainerInitializer.newInstance();
				if ( !annRef.get().enableDiscovery() ) {
					initializer.disableDiscovery();
				}
				initializer.addBeanClasses( annRef.get().beanClasses() );
				return initializer.initialize();
			} );
			extensionStore.put( CONTEXT_KEY, scope );
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		final Optional<CdiContainer> annRef = AnnotationSupport.findAnnotation(
				context.getRequiredTestMethod(),
				CdiContainer.class
		);

		if ( annRef.isEmpty() ) {
			// assume the annotations are defined on the class-level...
			return;
		}

		final ExtensionContext.Store extensionStore = locateExtensionStore( context.getRequiredTestInstance(), context );
		var scope = new CdiContainerScope( () -> {
			final SeContainerInitializer initializer = SeContainerInitializer.newInstance();
			if ( !annRef.get().enableDiscovery() ) {
				initializer.disableDiscovery();
			}
			initializer.addBeanClasses( annRef.get().beanClasses() );
			return initializer.initialize();
		} );
		extensionStore.put( CONTEXT_KEY, scope );
	}

	private static ExtensionContext.Store locateExtensionStore(Object testInstance, ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( CdiContainerExtension.class, context, testInstance );
	}
}
