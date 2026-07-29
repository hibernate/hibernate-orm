package org.hibernate.boot.beanvalidation;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.ValidationConstraintDdlInfluence;

/**
 * Legacy settings holder retained for compatibility.  Bean Validation is now
 * prepared as a {@link BeanValidationPlan} so its relational and runtime phases
 * can run at their proper construction boundaries.
 *
 * @author Steve Ebersole
 *
 * @deprecated Use {@linkplain BeanValidationPlan} instead.  This class is no longer used internally and
 * will be removed.
 */
@Deprecated(since = "9.0", forRemoval = true)
public class BeanValidationIntegrator implements BeanValidationSettings {

	/**
	 * Used to validate the type of an explicitly passed ValidatorFactory instance.
	 */
	public static void validateFactory(Object object) {
		try {
			final var activatorClass =
					BeanValidationIntegrator.class.getClassLoader().loadClass( ACTIVATOR_CLASS_NAME );
			try {
				activatorClass
						.getMethod( VALIDATE_SUPPLIED_FACTORY_METHOD_NAME, Object.class )
						.invoke( null, object );
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

	static void validateMissingBeanValidationApi(
			Set<ValidationMode> modes,
			ValidationConstraintDdlInfluence constraintInfluence) {
		BeanValidationPlan.validateMissingBeanValidationApi( modes, constraintInfluence );
	}

	public static void applyRelationalConstraints(
			Metadata metadata,
			ServiceRegistry serviceRegistry,
			Object validatorFactory) {
		final var influence = ValidationConstraintDdlInfluence.resolve( serviceRegistry, getValidationModes( serviceRegistry ) );
		if ( influence == ValidationConstraintDdlInfluence.DISABLED ) {
			return;
		}
		final var classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		if ( !isBeanValidationApiAvailable( classLoaderService ) ) {
			validateMissingBeanValidationApi( Set.of(), influence );
			return;
		}
		try {
			loadTypeSafeActivatorClass( classLoaderService )
					.getMethod( "applyRelationalConstraints", Metadata.class, ServiceRegistry.class,
							Object.class, ValidationConstraintDdlInfluence.class )
					.invoke( null, metadata, serviceRegistry, validatorFactory, influence );
		}
		catch (InvocationTargetException e) {
			final var cause = e.getTargetException();
			if ( cause instanceof HibernateException exception ) {
				throw exception;
			}
			if ( cause instanceof Error error ) {
				throw error;
			}
			throw new IntegrationException( "Error applying Bean Validation constraints to schema metadata", cause );
		}
		catch (ReflectiveOperationException e) {
			throw new IntegrationException( "Unable to apply Bean Validation constraints to schema metadata", e );
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

	private static boolean isBeanValidationApiAvailable(ClassLoaderService classLoaderService) {
		try {
			classLoaderService.classForName( JAKARTA_BV_CHECK_CLASS );
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	private static Class<?> loadTypeSafeActivatorClass(ClassLoaderService classLoaderService) {
		try {
			return classLoaderService.classForName( ACTIVATOR_CLASS_NAME );
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to load TypeSafeActivator class", e );
		}
	}
}
