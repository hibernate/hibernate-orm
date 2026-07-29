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
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.ValidationConstraintDdlInfluence;

import static org.hibernate.boot.beanvalidation.BeanValidationLogger.BEAN_VALIDATION_LOGGER;

/// Prepared Bean Validation integration split into its 2 separate activation phases -
///
/// * [#contributeRelationalConstraints] - installs DDL constraints via the boot-model
/// * [#activateCallbacks] - installs callback listeners with the SessionFactory for runtime validation
///
/// @since 9.0
/// @author Steve Ebersole
public final class BeanValidationPlan implements BeanValidationSettings {
	private static final String ACTIVATOR_CLASS_NAME = "org.hibernate.boot.beanvalidation.TypeSafeActivator";

	private final Metadata metadata;
	private final SessionFactoryOptions options;
	private final ServiceRegistry serviceRegistry;
	private final Set<ValidationMode> validationModes;
	private final ValidationConstraintDdlInfluence constraintInfluence;
	private final Class<?> activatorClass;
	private final Object validatorFactory;

	private BeanValidationPlan(
			Metadata metadata,
			SessionFactoryOptions options,
			ServiceRegistry serviceRegistry,
			Set<ValidationMode> validationModes,
			ValidationConstraintDdlInfluence constraintInfluence,
			Class<?> activatorClass,
			Object validatorFactory) {
		this.metadata = metadata;
		this.options = options;
		this.serviceRegistry = serviceRegistry;
		this.validationModes = validationModes;
		this.constraintInfluence = constraintInfluence;
		this.activatorClass = activatorClass;
		this.validatorFactory = validatorFactory;
	}

	public static BeanValidationPlan prepare(
			Metadata metadata,
			SessionFactoryOptions options,
			ServiceRegistry serviceRegistry) {
		final var modes = validationModes( serviceRegistry );
		final var influence = ValidationConstraintDdlInfluence.resolve( serviceRegistry, modes );
		if ( modes.size() > 1 ) {
			BEAN_VALIDATION_LOGGER.multipleValidationModes( ValidationMode.loggable( modes ) );
		}

		final var classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		if ( !isBeanValidationApiAvailable( classLoaderService ) ) {
			validateMissingBeanValidationApi( modes, influence );
			return new BeanValidationPlan( metadata, options, serviceRegistry, modes, influence, null, null );
		}

		final Class<?> activatorClass;
		try {
			activatorClass = classLoaderService.classForName( ACTIVATOR_CLASS_NAME );
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to load TypeSafeActivator class", e );
		}
		final var context = new PlanActivationContext(
				modes,
				influence,
				metadata,
				options,
				null,
				serviceRegistry
		);
		final var validatorFactory = invoke( activatorClass, "prepareValidatorFactory", context );
		return new BeanValidationPlan(
				metadata,
				options,
				serviceRegistry,
				modes,
				influence,
				activatorClass,
				validatorFactory
		);
	}

	/// Apply validation constraints to the relational boot model.  This must run
	/// before final metadata ordering and validation.
	public void contributeRelationalConstraints() {
		if ( validatorFactory != null ) {
			invoke(
					activatorClass,
					"applyRelationalConstraints",
					validatorFactory,
					activationContext( null )
			);
		}
	}

	/// Install runtime validation callbacks once the in-flight factory can own
	/// event listeners and observers.
	public void activateCallbacks(SessionFactoryImplementor sessionFactory) {
		if ( validatorFactory != null ) {
			invoke(
					activatorClass,
					"applyCallbackListeners",
					validatorFactory,
					activationContext( sessionFactory )
			);
		}
	}

	private ActivationContext activationContext(SessionFactoryImplementor sessionFactory) {
		return new PlanActivationContext(
				validationModes,
				constraintInfluence,
				metadata,
				options,
				sessionFactory,
				serviceRegistry
		);
	}

	private static Set<ValidationMode> validationModes(ServiceRegistry serviceRegistry) {
		final var settings = serviceRegistry.requireService( ConfigurationService.class ).getSettings();
		Object modeSetting = settings.get( JAKARTA_MODE_PROPERTY );
		if ( modeSetting == null ) {
			modeSetting = settings.get( MODE_PROPERTY );
		}
		return ValidationMode.parseValidationModes( modeSetting );
	}

	private static boolean isBeanValidationApiAvailable(ClassLoaderService classLoaderService) {
		try {
			classLoaderService.classForName( JAKARTA_BV_CHECK_CLASS );
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	private static Object invoke(Class<?> activatorClass, String methodName, ActivationContext context) {
		try {
			return activatorClass.getMethod( methodName, ActivationContext.class ).invoke( null, context );
		}
		catch (InvocationTargetException e) {
			throw rethrow( methodName, e.getTargetException() );
		}
		catch (Exception e) {
			throw new IntegrationException( "Unable to invoke TypeSafeActivator#" + methodName, e );
		}
	}

	private static Object invoke(
			Class<?> activatorClass,
			String methodName,
			Object validatorFactory,
			ActivationContext context) {
		try {
			return activatorClass
					.getMethod( methodName, Object.class, ActivationContext.class )
					.invoke( null, validatorFactory, context );
		}
		catch (InvocationTargetException e) {
			throw rethrow( methodName, e.getTargetException() );
		}
		catch (Exception e) {
			throw new IntegrationException( "Unable to invoke TypeSafeActivator#" + methodName, e );
		}
	}

	private static RuntimeException rethrow(String methodName, Throwable throwable) {
		return throwable instanceof RuntimeException runtimeException
				? runtimeException
				: new IntegrationException( "Error invoking TypeSafeActivator#" + methodName, throwable );
	}

	private record PlanActivationContext(
			Set<ValidationMode> validationModes,
			ValidationConstraintDdlInfluence constraintInfluence,
			Metadata metadata,
			SessionFactoryOptions sessionFactoryOptions,
			SessionFactoryImplementor sessionFactory,
			ServiceRegistry serviceRegistry)
			implements ActivationContext {

		@Override
		public Set<ValidationMode> getValidationModes() {
			return validationModes;
		}

		@Override
		public ValidationConstraintDdlInfluence getValidationConstraintDdlInfluence() {
			return constraintInfluence;
		}

		@Override
		public Metadata getMetadata() {
			return metadata;
		}

		@Override
		public SessionFactoryOptions getSessionFactoryOptions() {
			return sessionFactoryOptions;
		}

		@Override
		public SessionFactoryImplementor getSessionFactory() {
			return sessionFactory;
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return serviceRegistry;
		}
	}

	static void validateMissingBeanValidationApi(
			Set<ValidationMode> modes,
			ValidationConstraintDdlInfluence constraintInfluence) {
		if ( modes.contains( ValidationMode.CALLBACK ) ) {
			throw new IntegrationException(
					"Jakarta Validation API was not available, but 'callback' validation was requested"
			);
		}
		if ( constraintInfluence == ValidationConstraintDdlInfluence.REQUIRED ) {
			throw new IntegrationException(
					"Bean Validation API was not available, but '"
					+ SchemaToolingSettings.APPLY_VALIDATION_CONSTRAINTS
					+ "' was set to 'REQUIRED'"
			);
		}
	}
}
