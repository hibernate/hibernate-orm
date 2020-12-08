/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.integrator.spi.Integrator;
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

		ServiceRegistryScopeImpl existingScope = (ServiceRegistryScopeImpl) store.get( REGISTRY_KEY );

		if ( existingScope == null ) {
			final ServiceRegistryScopeImpl scope = new ServiceRegistryScopeImpl(  );
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
						configureIntegrators(serviceRegistryAnn, scope);
					}

					return ssrb.build();
				};
			}



			scope.createRegistry(producer);

			locateExtensionStore( testInstance, context ).put( REGISTRY_KEY, scope );

			if ( testInstance instanceof ServiceRegistryScopeAware ) {
				( (ServiceRegistryScopeAware) testInstance ).injectServiceRegistryScope( scope );
			}
			return scope;
		}

		return existingScope;
	}

	private static void configureIntegrators(
			ServiceRegistry serviceRegistryAnn,
			final ServiceRegistryScopeImpl serviceRegistryScope) {
		for ( Class<? extends Integrator> integrator : serviceRegistryAnn.integrators() ) {
			serviceRegistryScope.applyIntegrator( integrator );
		}
	}

	private static void configureServices(ServiceRegistry serviceRegistryAnn, StandardServiceRegistryBuilder ssrb) {
		try {
			for ( Setting setting : serviceRegistryAnn.settings() ) {
				ssrb.applySetting( setting.name(), setting.value() );
			}

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
		private ServiceRegistryProducer producer;

		private StandardServiceRegistry registry;
		private boolean active = true;
		private List<Class<? extends Integrator>> integrators = new ArrayList<>();

		public ServiceRegistryScopeImpl() {

		}

		public StandardServiceRegistry createRegistry(ServiceRegistryProducer producer) {
			this.producer = producer;
			verifyActive();
			BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder().enableAutoClose();
			integrators.forEach(
					integrator -> {
						try {
							bootstrapServiceRegistryBuilder.applyIntegrator( integrator.newInstance() );
						}
						catch (Exception e) {
							throw new RuntimeException( "Could not configure BootstrapServiceRegistryBuilder", e );
						}
					}
			);

			final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder(bootstrapServiceRegistryBuilder.build());
			// we will close it ourselves explicitly.
			ssrb.disableAutoClose();

			return producer.produceServiceRegistry( ssrb );
		}

		private void verifyActive() {
			if ( !active ) {
				throw new IllegalStateException( "ServiceRegistryScope no longer active" );
			}
		}

		public void applyIntegrator(Class<? extends Integrator> integrator) {
			integrators.add( integrator );
		}

		@Override
		public StandardServiceRegistry getRegistry() {
			verifyActive();

			if ( registry == null ) {
				registry = createRegistry( producer );
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
