/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm;

import java.util.Optional;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * JUnit extension used to manage the StandardServiceRegistry used by a test including
 * creating the StandardServiceRegistry and releasing it afterwards
 *
 * @author Steve Ebersole
 */
public class ServiceRegistryExtension implements TestInstancePostProcessor, AfterAllCallback {
	private static final String REGISTRY_KEY = StandardServiceRegistry.class.getName();

	private static ExtensionContext.Store locateExtensionStore(Object testInstance, ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( ServiceRegistryExtension.class, context, testInstance );
	}

	public static Optional<StandardServiceRegistry> findServiceRegistry(Object testInstance, ExtensionContext context) {
		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		return Optional.ofNullable( (StandardServiceRegistry) store.get( REGISTRY_KEY ) );
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
		if ( !context.getElement().isPresent() ) {
			throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
		}

		final Optional<ServiceRegistry> serviceRegistryAnnWrapper = AnnotationSupport.findAnnotation(
				context.getElement().get(),
				ServiceRegistry.class
		);

		final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();

		if ( serviceRegistryAnnWrapper.isPresent() ) {
			final ServiceRegistry serviceRegistryAnn = serviceRegistryAnnWrapper.get();
			configureServices( serviceRegistryAnn, ssrb );
		}

		final StandardServiceRegistry serviceRegistry = ssrb.build();

		locateExtensionStore( testInstance, context ).put( REGISTRY_KEY, serviceRegistry );

		if ( testInstance instanceof ServiceRegistryAware ) {
			( (ServiceRegistryAware) testInstance ).injectServiceRegistry( serviceRegistry );
		}
	}

	private void configureServices(ServiceRegistry serviceRegistryAnn, StandardServiceRegistryBuilder ssrb) throws Exception {
		for ( Class<? extends ServiceContributor> contributorClass : serviceRegistryAnn.serviceContributors() ) {
			final ServiceContributor serviceContributor = contributorClass.newInstance();
			serviceContributor.contribute( ssrb );
		}

		for ( Class<? extends StandardServiceInitiator> initiatorClass : serviceRegistryAnn.initiators() ) {
			ssrb.addInitiator( initiatorClass.newInstance() );
		}

		for ( ServiceRegistry.Service service : serviceRegistryAnn.services() ) {
			ssrb.addService( service.role(), service.impl().newInstance() );
		}
	}

	@Override
	public void afterAll(ExtensionContext context) {
		final Object testInstance = context.getRequiredTestInstance();

		if ( testInstance instanceof ServiceRegistryAware ) {
			( (ServiceRegistryAware) testInstance ).injectServiceRegistry( null );
		}

		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		final StandardServiceRegistry registry = (StandardServiceRegistry) store.remove( REGISTRY_KEY );
		if ( registry != null ) {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}
}
