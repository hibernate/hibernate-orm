/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.Optional;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;

import org.jboss.logging.Logger;

/**
 * JUnit extension used to manage the StandardServiceRegistry used by a test including
 * creating the StandardServiceRegistry and releasing it afterwards
 *
 * @author Steve Ebersole
 */
public class ServiceRegistryExtension
		implements TestInstancePostProcessor, AfterAllCallback, TestExecutionExceptionHandler {
	private static final Logger log = Logger.getLogger( ServiceRegistryExtension.class );
	private static final String REGISTRY_KEY = ServiceRegistryScope.class.getName();

	@SuppressWarnings("WeakerAccess")
	public static StandardServiceRegistry findServiceRegistry(
			Object testInstance,
			ExtensionContext context) {
		return findServiceRegistryScope( testInstance, context ).getRegistry();
	}

	private static ExtensionContext.Store locateExtensionStore(
			Object testInstance,
			ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( ServiceRegistryExtension.class, context, testInstance );
	}

	@SuppressWarnings("WeakerAccess")
	public static ServiceRegistryScope findServiceRegistryScope(Object testInstance, ExtensionContext context) {
		log.tracef( "#findServiceRegistryScope(%s, %s)", testInstance, context.getDisplayName() );

		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );

		ServiceRegistryScopeImpl scope = (ServiceRegistryScopeImpl) store.get( REGISTRY_KEY );

		if ( scope == null ) {
			log.debugf( "Creating ServiceRegistryScope - %s", context.getDisplayName() );

			final ServiceRegistryProducer producer;

			if ( testInstance instanceof ServiceRegistryProducer ) {
				producer = (ServiceRegistryProducer) testInstance;
			}
			else {
				producer = ssrb -> {
					if ( !context.getElement().isPresent() ) {
						throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
					}

					final Optional<ServiceRegistry> serviceRegistryAnnWrapper = AnnotationSupport.findAnnotation(
							context.getElement().get(),
							ServiceRegistry.class
					);

					if ( serviceRegistryAnnWrapper.isPresent() ) {
						final ServiceRegistry serviceRegistryAnn = serviceRegistryAnnWrapper.get();
						configureServices( serviceRegistryAnn, ssrb );
					}

					return ssrb.build();
				};
			}

			scope = new ServiceRegistryScopeImpl( producer );

			locateExtensionStore( testInstance, context ).put( REGISTRY_KEY, scope );

			if ( testInstance instanceof ServiceRegistryScopeAware ) {
				( (ServiceRegistryScopeAware) testInstance ).injectServiceRegistryScope( scope );
			}
		}

		return scope;
	}

	private static void configureServices(ServiceRegistry serviceRegistryAnn, StandardServiceRegistryBuilder ssrb) {
		try {
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
		catch (Exception e) {
			throw new RuntimeException( "Could not configure StandardServiceRegistryBuilder", e );
		}
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
		log.tracef( "#postProcessTestInstance(%s, %s)", testInstance, context.getDisplayName() );

		findServiceRegistryScope( testInstance, context );
	}

	@Override
	public void afterAll(ExtensionContext context) {
		log.tracef( "#afterAll(%s)", context.getDisplayName() );

		final Object testInstance = context.getRequiredTestInstance();

		if ( testInstance instanceof ServiceRegistryScopeAware ) {
			( (ServiceRegistryScopeAware) testInstance ).injectServiceRegistryScope( null );
		}

		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		final ServiceRegistryScopeImpl scope = (ServiceRegistryScopeImpl) store.remove( REGISTRY_KEY );
		if ( scope != null ) {
			scope.close();
		}
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		log.tracef( "#handleTestExecutionException(%s, %s)", context.getDisplayName(), throwable );

		final Object testInstance = context.getRequiredTestInstance();
		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		final ServiceRegistryScopeImpl scope = (ServiceRegistryScopeImpl) store.get( REGISTRY_KEY );
		scope.releaseRegistry();

		throw throwable;
	}

	private static class ServiceRegistryScopeImpl implements ServiceRegistryScope, ExtensionContext.Store.CloseableResource {
		private final ServiceRegistryProducer producer;

		private StandardServiceRegistry registry;
		private boolean active = true;

		public ServiceRegistryScopeImpl(ServiceRegistryProducer producer) {
			this.producer = producer;

			this.registry = createRegistry();
		}

		private StandardServiceRegistry createRegistry() {
			verifyActive();

			final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
			// we will close it ourselves explicitly.
			ssrb.disableAutoClose();

			return producer.produceServiceRegistry( ssrb );
		}

		private void verifyActive() {
			if ( !active ) {
				throw new IllegalStateException( "ServiceRegistryScope no longer active" );
			}
		}

		@Override
		public StandardServiceRegistry getRegistry() {
			verifyActive();

			if ( registry == null ) {
				registry = createRegistry();
			}

			return registry;
		}

		@Override
		public void close() {
			if ( ! active ) {
				return;
			}

			log.debugf( "Closing ServiceRegistryScope" );

			active = false;

			if ( registry != null ) {
				releaseRegistry();
				registry = null;
			}
		}

		private void releaseRegistry() {
			if ( registry == null ) {
				return;
			}

			try {
				log.tracef( "#releaseRegistry" );
				StandardServiceRegistryBuilder.destroy( registry );
			}
			catch (Exception e) {
				log.warn( "Unable to release StandardServiceRegistry", e );
			}
			finally {
				registry = null;
			}
		}
	}
}
