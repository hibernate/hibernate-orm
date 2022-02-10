/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.ServiceContributor;

import org.hibernate.testing.boot.ExtraJavaServicesClassLoaderService;
import org.hibernate.testing.boot.ExtraJavaServicesClassLoaderService.JavaServiceDescriptor;
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

	public static ServiceRegistryScope findServiceRegistryScope(Object testInstance, ExtensionContext context) {
		log.tracef( "#findServiceRegistryScope(%s, %s)", testInstance, context.getDisplayName() );

		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );

		ServiceRegistryScopeImpl existingScope = (ServiceRegistryScopeImpl) store.get( REGISTRY_KEY );

		if ( existingScope == null ) {
			log.debugf( "Creating ServiceRegistryScope - %s", context.getDisplayName() );

			final BootstrapServiceRegistryProducer bsrProducer;

			final Optional<BootstrapServiceRegistry> bsrAnnWrapper = AnnotationSupport.findAnnotation(
					context.getElement().get(),
					BootstrapServiceRegistry.class
			);

			if ( bsrAnnWrapper.isPresent() ) {
				bsrProducer = bsrBuilder -> {
					final BootstrapServiceRegistry bsrAnn = bsrAnnWrapper.get();
					configureJavaServices( bsrAnn, bsrBuilder );
					configureIntegrators( bsrAnn, bsrBuilder );

					return bsrBuilder.enableAutoClose().build();
				};
			}
			else {
				bsrProducer = BootstrapServiceRegistryBuilder::build;
			}

			final ServiceRegistryProducer ssrProducer;

			if ( testInstance instanceof ServiceRegistryProducer ) {
				ssrProducer = (ServiceRegistryProducer) testInstance;
			}
			else {
				ssrProducer = new ServiceRegistryProducerImpl(context);
			}

			final ServiceRegistryScopeImpl scope = new ServiceRegistryScopeImpl( bsrProducer, ssrProducer );
			scope.getRegistry();

			locateExtensionStore( testInstance, context ).put( REGISTRY_KEY, scope );

			if ( testInstance instanceof ServiceRegistryScopeAware ) {
				( (ServiceRegistryScopeAware) testInstance ).injectServiceRegistryScope( scope );
			}
			return scope;
		}

		return existingScope;
	}

	private static class ServiceRegistryProducerImpl implements ServiceRegistryProducer{
		private final ExtensionContext context;
		public ServiceRegistryProducerImpl(ExtensionContext context) {
			this.context = context;
			if ( !context.getElement().isPresent() ) {
				throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
			}
		}

		@Override
		public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder ssrb) {
			// set some baseline test settings
			ssrb.applySetting( AvailableSettings.STATEMENT_INSPECTOR, org.hibernate.testing.jdbc.SQLStatementInspector.class );

			final Optional<ServiceRegistry> ssrAnnWrapper = AnnotationSupport.findAnnotation(
					context.getElement().get(),
					ServiceRegistry.class
			);

			if ( ssrAnnWrapper.isPresent() ) {
				final ServiceRegistry serviceRegistryAnn = ssrAnnWrapper.get();
				configureServices( serviceRegistryAnn, ssrb );
			}

			return ssrb.build();
		}

		@Override
		public void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder bsrb) {

		}
	}

	private static void configureIntegrators(
			BootstrapServiceRegistry bsrAnn,
			final BootstrapServiceRegistryBuilder bsrBuilder) {
		final Class<? extends Integrator>[] integrators = bsrAnn.integrators();
		if ( integrators.length == 0 ) {
			return;
		}

		for ( Class<? extends Integrator> integratorImpl : integrators ) {
			assert integratorImpl != null;

			try {
				final Constructor<? extends Integrator> constructor = integratorImpl.getDeclaredConstructor();

				final Integrator integrator = constructor.newInstance();
				bsrBuilder.applyIntegrator( integrator );
			}
			catch (NoSuchMethodException e) {
				throw new IllegalArgumentException( "Could not find no-arg constructor for Integrator : " + integratorImpl.getName(), e );
			}
			catch (IllegalAccessException e) {
				throw new IllegalArgumentException( "Unable to access no-arg constructor for Integrator : " + integratorImpl.getName(), e );
			}
			catch (InstantiationException | InvocationTargetException e) {
				throw new IllegalArgumentException( "Unable to instantiate Integrator : " + integratorImpl.getName(), e );
			}
		}
	}

	private static void configureJavaServices(BootstrapServiceRegistry bsrAnn, BootstrapServiceRegistryBuilder bsrBuilder) {
		final BootstrapServiceRegistry.JavaService[] javaServiceAnns = bsrAnn.javaServices();
		if ( javaServiceAnns.length == 0 ) {
			return;
		}

		final List<JavaServiceDescriptor<?>> javaServiceDescriptors = new ArrayList<>( javaServiceAnns.length );
		for ( int i = 0; i < javaServiceAnns.length; i++ ) {
			final BootstrapServiceRegistry.JavaService javaServiceAnn = javaServiceAnns[ i ];
			javaServiceDescriptors.add(
					new JavaServiceDescriptor(
							javaServiceAnn.role(),
							javaServiceAnn.impl()
					)
			);
		}
		final ExtraJavaServicesClassLoaderService cls = new ExtraJavaServicesClassLoaderService( javaServiceDescriptors );
		bsrBuilder.applyClassLoaderService( cls );
	}

	private static void configureServices(ServiceRegistry serviceRegistryAnn, StandardServiceRegistryBuilder ssrb) {
		try {
			for ( Setting setting : serviceRegistryAnn.settings() ) {
				ssrb.applySetting( setting.name(), setting.value() );
			}

			for ( SettingProvider providerAnn : serviceRegistryAnn.settingProviders() ) {
				final Class<? extends SettingProvider.Provider> providerImpl = providerAnn.provider();
				final SettingProvider.Provider<?> provider = providerImpl.getConstructor().newInstance();
				ssrb.applySetting( providerAnn.settingName(), provider.getSetting() );
			}

			for ( Class<? extends ServiceContributor> contributorClass : serviceRegistryAnn.serviceContributors() ) {
				final ServiceContributor serviceContributor = contributorClass.newInstance();
				serviceContributor.contribute( ssrb );
			}

			for ( Class<? extends StandardServiceInitiator> initiatorClass : serviceRegistryAnn.initiators() ) {
				ssrb.addInitiator( initiatorClass.newInstance() );
			}

			for ( ServiceRegistry.Service service : serviceRegistryAnn.services() ) {
				ssrb.addService( (Class) service.role(), service.impl().newInstance() );
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
		private BootstrapServiceRegistryProducer bsrProducer;
		private ServiceRegistryProducer ssrProducer;

		private StandardServiceRegistry registry;
		private boolean active = true;

		public ServiceRegistryScopeImpl(BootstrapServiceRegistryProducer bsrProducer, ServiceRegistryProducer ssrProducer) {
			this.bsrProducer = bsrProducer;
			this.ssrProducer = ssrProducer;
		}

		private StandardServiceRegistry createRegistry() {
			BootstrapServiceRegistryBuilder bsrb = new BootstrapServiceRegistryBuilder().enableAutoClose();
			ssrProducer.prepareBootstrapRegistryBuilder(bsrb);

			final org.hibernate.boot.registry.BootstrapServiceRegistry bsr = bsrProducer.produceServiceRegistry( bsrb );
			try {
				final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder( bsr );
				// we will close it ourselves explicitly.
				ssrb.disableAutoClose();

				return registry = ssrProducer.produceServiceRegistry( ssrb );
			}
			catch (Throwable t) {
				bsr.close();
				throw t;
			}
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
