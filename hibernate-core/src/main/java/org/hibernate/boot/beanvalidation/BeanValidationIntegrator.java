/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.beanvalidation;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import static org.hibernate.boot.beanvalidation.BeanValidationLogger.BEAN_VALIDATION_LOGGER;

/**
 * In {@link Integrator} for Bean Validation.
 *
 * @author Steve Ebersole
 */
public class BeanValidationIntegrator implements Integrator {

	public static final String APPLY_CONSTRAINTS = "hibernate.validator.apply_to_ddl";

	public static final String JAKARTA_BV_CHECK_CLASS = "jakarta.validation.ConstraintViolation";

	public static final String MODE_PROPERTY = "javax.persistence.validation.mode";
	public static final String JAKARTA_MODE_PROPERTY = "jakarta.persistence.validation.mode";

	private static final String ACTIVATOR_CLASS_NAME = "org.hibernate.boot.beanvalidation.TypeSafeActivator";
	private static final String VALIDATE_SUPPLIED_FACTORY_METHOD_NAME = "validateSuppliedFactory";
	private static final String ACTIVATE_METHOD_NAME = "activate";

	/**
	 * Used to validate the type of an explicitly passed ValidatorFactory instance
	 *
	 * @param object The supposed ValidatorFactory instance
	 */
	public static void validateFactory(Object object) {
		try {
			// this direct usage of ClassLoader should be fine since the classes exist in the same jar
			final var activatorClass =
					BeanValidationIntegrator.class.getClassLoader()
							.loadClass( ACTIVATOR_CLASS_NAME );
			try {
				final var validateMethod =
						activatorClass.getMethod( VALIDATE_SUPPLIED_FACTORY_METHOD_NAME, Object.class );
				try {
					validateMethod.invoke( null, object );
				}
				catch (InvocationTargetException e) {
					if ( e.getTargetException() instanceof HibernateException exception ) {
						throw exception;
					}
					throw new HibernateException( "Unable to check validity of passed ValidatorFactory", e );
				}
				catch (IllegalAccessException e) {
					throw new HibernateException( "Unable to check validity of passed ValidatorFactory", e );
				}
			}
			catch (HibernateException e) {
				throw e;
			}
			catch (Exception e) {
				throw new HibernateException( "Could not locate method needed for ValidatorFactory validation", e );
			}
		}
		catch (HibernateException e) {
			throw e;
		}
		catch (Exception e) {
			throw new HibernateException( "Could not locate TypeSafeActivator class", e );
		}
	}

	@Override
	public void integrate(
			Metadata metadata,
			BootstrapContext bootstrapContext,
			SessionFactoryImplementor sessionFactory) {
		final var serviceRegistry = sessionFactory.getServiceRegistry();
		// IMPL NOTE: see the comments on ActivationContext.getValidationModes() as to why this is multi-valued...
		final var modes = getValidationModes( serviceRegistry );
		switch ( modes.size() ) {
			case 0:
				// should never happen, since getValidationModes()
				// always returns at least one mode
				return;
			case 1:
				if ( modes.contains( ValidationMode.NONE ) ) {
					// we have nothing to do; just return
					return;
				}
				break;
			default:
				BEAN_VALIDATION_LOGGER.multipleValidationModes( ValidationMode.loggable( modes ) );
		}
		activate( metadata, sessionFactory, serviceRegistry, modes );
	}

	private void activate(Metadata metadata, SessionFactoryImplementor sessionFactory, ServiceRegistryImplementor serviceRegistry, Set<ValidationMode> modes) {
		final var classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		// see if the Bean Validation API is available on the classpath
		if ( isBeanValidationApiAvailable( classLoaderService ) ) {
			// and if so, call out to the TypeSafeActivator
			try {
				final var activationContext =
						new ActivationContextImpl( modes, metadata, sessionFactory,
								(SessionFactoryServiceRegistry) serviceRegistry );
				callActivateMethod( classLoaderService, activationContext );
			}
			catch (NoSuchMethodException e) {
				throw new HibernateException( "Unable to locate TypeSafeActivator#activate method", e );
			}
		}
		else {
			// otherwise check the validation modes
			// todo : in many ways this duplicates the checks done on the TypeSafeActivator when a ValidatorFactory could not be obtained
			validateMissingBeanValidationApi( modes );
		}
	}

	private void callActivateMethod(ClassLoaderService classLoaderService, ActivationContext activationContext)
			throws NoSuchMethodException {
		final var activateMethod =
				loadTypeSafeActivatorClass( classLoaderService )
						.getMethod( ACTIVATE_METHOD_NAME, ActivationContext.class );
		try {
			activateMethod.invoke( null, activationContext );
		}
		catch (InvocationTargetException e) {
			final var targetException = e.getTargetException();
			throw targetException instanceof HibernateException exception
					? exception
					: new IntegrationException( "Error activating Bean Validation integration",
							targetException );
		}
		catch (Exception e) {
			throw new IntegrationException( "Error activating Bean Validation integration", e );
		}
	}

	private static Set<ValidationMode> getValidationModes(ServiceRegistry serviceRegistry) {
		final var settings =
				serviceRegistry.requireService( ConfigurationService.class )
						.getSettings();
		Object modeSetting = settings.get( JAKARTA_MODE_PROPERTY );
		if ( modeSetting == null ) {
			modeSetting = settings.get( MODE_PROPERTY );
		}
		return ValidationMode.parseValidationModes( modeSetting );
	}

	private boolean isBeanValidationApiAvailable(ClassLoaderService classLoaderService) {
		try {
			classLoaderService.classForName( JAKARTA_BV_CHECK_CLASS );
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Used to validate the case when the Bean Validation API is not available.
	 *
	 * @param modes The requested validation modes.
	 */
	private void validateMissingBeanValidationApi(Set<ValidationMode> modes) {
		if ( modes.contains( ValidationMode.CALLBACK ) ) {
			throw new IntegrationException( "Bean Validation API was not available, but 'callback' validation was requested" );
		}
		if ( modes.contains( ValidationMode.DDL ) ) {
			throw new IntegrationException( "Bean Validation API was not available, but 'ddl' validation was requested" );
		}
	}

	private Class<?> loadTypeSafeActivatorClass(ClassLoaderService classLoaderService) {
		try {
			return classLoaderService.classForName( ACTIVATOR_CLASS_NAME );
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to load TypeSafeActivator class", e );
		}
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// nothing to do here afaik
	}

	private record ActivationContextImpl(
			Set<ValidationMode> modes,
			Metadata metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry)
				implements ActivationContext {

		@Override
		public Set<ValidationMode> getValidationModes() {
			return modes;
		}

		@Override
		public Metadata getMetadata() {
			return metadata;
		}

		@Override
		public SessionFactoryImplementor getSessionFactory() {
			return sessionFactory;
		}

		@Override
		public SessionFactoryServiceRegistry getServiceRegistry() {
			return serviceRegistry;
		}
	}
}
