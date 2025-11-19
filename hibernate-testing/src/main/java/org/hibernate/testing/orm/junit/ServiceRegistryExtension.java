/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.testing.boot.ExtraJavaServicesClassLoaderService;
import org.hibernate.testing.boot.ExtraJavaServicesClassLoaderService.JavaServiceDescriptor;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// JUnit extension used to manage the StandardServiceRegistry used by a test including
/// creating the StandardServiceRegistry and releasing it afterwards
///
/// @see BootstrapServiceRegistry
/// @see ServiceRegistry
/// @see ServiceRegistryFunctionalTesting
/// @see ServiceRegistryProducer
/// @see ServiceRegistryScopeAware
/// @see ServiceRegistryParameterResolver
///
/// @author Steve Ebersole
public class ServiceRegistryExtension
		implements TestInstancePostProcessor, BeforeEachCallback, TestExecutionExceptionHandler {
	private static final Logger log = Logger.getLogger( ServiceRegistryExtension.class );
	private static final String REGISTRY_KEY = ServiceRegistryScope.class.getName();
	private static final String ADDITIONAL_SETTINGS_KEY = ServiceRegistryExtension.class.getName() + "#ADDITIONAL_SETTINGS";

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
		log.tracef( "#postProcessTestInstance(%s, %s)", testInstance, context.getDisplayName() );

		assert context.getTestClass().isPresent();

		final Optional<BootstrapServiceRegistry> bsrAnnRef = AnnotationSupport.findAnnotation(
				context.getElement().get(),
				BootstrapServiceRegistry.class
		);
		final Optional<ServiceRegistry> ssrAnnRef = AnnotationSupport.findAnnotation(
				context.getElement().get(),
				ServiceRegistry.class
		);

		final ServiceRegistryScope created = createServiceRegistryScope( testInstance, bsrAnnRef, ssrAnnRef, context );
		locateExtensionStore( testInstance, context ).put( REGISTRY_KEY, created );
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		Optional<BootstrapServiceRegistry> bsrAnnRef = AnnotationSupport.findAnnotation(
				context.getElement().get(),
				BootstrapServiceRegistry.class
		);
		Optional<ServiceRegistry> ssrAnnRef = AnnotationSupport.findAnnotation(
				context.getElement().get(),
				ServiceRegistry.class
		);

		if ( bsrAnnRef.isEmpty() && ssrAnnRef.isEmpty() ) {
			// assume the annotations are defined on the class-level...
			// will be validated by the parameter-resolver or consuming extension
			return;
		}
		else if ( bsrAnnRef.isPresent() && ssrAnnRef.isPresent() ) {
			// the method has both - use them -> fall through
		}
		else if ( bsrAnnRef.isPresent() ) {
			// the method has BootstrapServiceRegistry but not ServiceRegistry
			//
			// see if there is a ServiceRegistry at the class-level:
			//		yes -> use this class-level one
			//		no -> treat it as implicit

			ssrAnnRef = AnnotationSupport.findAnnotation(
					context.getRequiredTestClass(),
					ServiceRegistry.class
			);
		}
		else if ( ssrAnnRef.isPresent() ) {
			// the method has ServiceRegistry but not BootstrapServiceRegistry
			//
			// see if there is a BootstrapServiceRegistry at the class-level:
			//		yes -> use this class-level one
			//		no -> treat it as implicit

			bsrAnnRef = AnnotationSupport.findAnnotation(
					context.getRequiredTestClass(),
					BootstrapServiceRegistry.class
			);
		}
		else {
			throw new RuntimeException( "Some clever text" );
		}

		final Object testInstance = context.getRequiredTestInstance();

		final ServiceRegistryScope created = createServiceRegistryScope( testInstance, bsrAnnRef, ssrAnnRef, context );
		final ExtensionContext.Store extensionStore = locateExtensionStore( testInstance, context );
		extensionStore.put( REGISTRY_KEY, created );
	}

	private static ExtensionContext.Store locateExtensionStore(
			Object testInstance,
			ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( ServiceRegistryExtension.class, context, testInstance );
	}

	public static ServiceRegistryScope findServiceRegistryScope(Object testInstance, ExtensionContext context) {
		log.tracef( "#findServiceRegistryScope(%s, %s)", testInstance, context.getDisplayName() );

		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		final ServiceRegistryScopeImpl existingScope = (ServiceRegistryScopeImpl) store.get( REGISTRY_KEY );

		if ( existingScope == null ) {
			throw new RuntimeException( "No ServiceRegistryScope known in context" );
		}

		return existingScope;
	}

	private static ServiceRegistryScopeImpl createServiceRegistryScope(
			Object testInstance,
			Optional<BootstrapServiceRegistry> bsrAnnRef,
			Optional<ServiceRegistry> ssrAnnRef,
			ExtensionContext context) {
		log.debugf( "Creating ServiceRegistryScope - %s", context.getDisplayName() );

		final BootstrapServiceRegistryProducer bsrProducer;

		//noinspection OptionalIsPresent
		if ( bsrAnnRef.isPresent() ) {
			bsrProducer = bsrBuilder -> {
				final BootstrapServiceRegistry bsrAnn = bsrAnnRef.get();
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
			ssrProducer = new ServiceRegistryProducerImpl( context, ssrAnnRef );
		}

		final ServiceRegistryScopeImpl scope = new ServiceRegistryScopeImpl( bsrProducer, ssrProducer );
		scope.getRegistry();

		if ( testInstance instanceof ServiceRegistryScopeAware ) {
			( (ServiceRegistryScopeAware) testInstance ).injectServiceRegistryScope( scope );
		}

		return scope;
	}

	private static class ServiceRegistryProducerImpl implements ServiceRegistryProducer {
		private final ExtensionContext extensionContext;
		private final Optional<ServiceRegistry> ssrAnnRef;

		public ServiceRegistryProducerImpl(ExtensionContext extensionContext, Optional<ServiceRegistry> ssrAnnRef) {
			this.extensionContext = extensionContext;
			this.ssrAnnRef = ssrAnnRef;
		}

		@Override
		public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder ssrb) {
			// set some baseline test settings
			ssrb.applySetting( AvailableSettings.STATEMENT_INSPECTOR, SQLStatementInspector.class );
			ssrb.applySetting( PersistentTableStrategy.DROP_ID_TABLES, "true" );
			ssrb.applySetting( GlobalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );
			ssrb.applySetting( LocalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );

			if ( ssrAnnRef.isPresent() ) {
				final ServiceRegistry serviceRegistryAnn = ssrAnnRef.get();
				configureServices( serviceRegistryAnn, ssrb, extensionContext );
			}
			ServiceRegistryUtil.applySettings( ssrb.getSettings() );

			return ssrb.build();
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

	private static void configureServices(
			ServiceRegistry serviceRegistryAnn,
			StandardServiceRegistryBuilder ssrb,
			ExtensionContext junitContext) {
		try {
			applyConfigurationSets( serviceRegistryAnn, ssrb );

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

			for ( ServiceRegistry.ResolvableSetting resolvableSetting : serviceRegistryAnn.resolvableSettings() ) {
				final Class<? extends ServiceRegistry.SettingResolver> resolverClass = resolvableSetting.resolver();
				ssrb.applySetting(
						resolvableSetting.settingName(),
						resolverClass.getDeclaredConstructor().newInstance().resolve( ssrb, junitContext )
				);
			}

		}
		catch (Exception e) {
			throw new RuntimeException( "Could not configure StandardServiceRegistryBuilder", e );
		}
	}

	private static void applyConfigurationSets(ServiceRegistry serviceRegistryAnn, StandardServiceRegistryBuilder ssrb) {
		final SettingConfiguration[] settingConfigurations = serviceRegistryAnn.settingConfigurations();
		for ( int i = 0; i < settingConfigurations.length; i++ ) {
			try {
				final SettingConfiguration.Configurer configurer = settingConfigurations[i].configurer().getDeclaredConstructor().newInstance();
				configurer.applySettings( ssrb );
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				throw new RuntimeException( e );
			}
		}
	}

//	@Override
//	public void afterAll(ExtensionContext context) {
//		log.tracef( "#afterAll(%s)", context.getDisplayName() );
//
//		final Object testInstance = context.getRequiredTestInstance();
//
//		if ( testInstance instanceof ServiceRegistryScopeAware ) {
//			( (ServiceRegistryScopeAware) testInstance ).injectServiceRegistryScope( null );
//		}
//
//		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
//		final ServiceRegistryScopeImpl scope = (ServiceRegistryScopeImpl) store.remove( REGISTRY_KEY );
//		if ( scope != null ) {
//			scope.close();
//		}
//	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		log.tracef( "#handleTestExecutionException(%s, %s)", context.getDisplayName(), throwable );

		final Object testInstance = context.getRequiredTestInstance();
		final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
		final ServiceRegistryScopeImpl scope = (ServiceRegistryScopeImpl) store.get( REGISTRY_KEY );
		scope.releaseRegistry();

		throw throwable;
	}

	private static class ServiceRegistryScopeImpl implements ServiceRegistryScope, AutoCloseable {
		private final BootstrapServiceRegistryProducer bsrProducer;
		private final ServiceRegistryProducer ssrProducer;
		private Map<String, Object> additionalSettings;

		private StandardServiceRegistry registry;
		private boolean active = true;

		public ServiceRegistryScopeImpl(BootstrapServiceRegistryProducer bsrProducer, ServiceRegistryProducer ssrProducer) {
			this.bsrProducer = bsrProducer;
			this.ssrProducer = ssrProducer;
		}

		private StandardServiceRegistry createRegistry() {
			BootstrapServiceRegistryBuilder bsrb = new BootstrapServiceRegistryBuilder().enableAutoClose();
			bsrb.applyClassLoader( Thread.currentThread().getContextClassLoader() );

			final org.hibernate.boot.registry.BootstrapServiceRegistry bsr = bsrProducer.produceServiceRegistry( bsrb );
			try {
				final StandardServiceRegistryBuilder ssrb = ServiceRegistryUtil.serviceRegistryBuilder( bsr );
				// we will close it ourselves explicitly.
				ssrb.disableAutoClose();

				if ( additionalSettings != null ) {
					ssrb.applySettings( additionalSettings );
				}

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
			if ( !active ) {
				return;
			}

			log.debugf( "Closing ServiceRegistryScope" );

			active = false;

			if ( registry != null ) {
				releaseRegistry();
				registry = null;
			}
		}

		@Override
		public void releaseRegistry() {
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
				additionalSettings = null;
			}
		}

		@Override
		public Map<String, Object> getAdditionalSettings() {
			final Map<String, Object> s;
			return (s = additionalSettings) == null ? (additionalSettings = new HashMap<>()) : s;
		}
	}
}
