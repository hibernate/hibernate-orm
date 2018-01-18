/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.beanvalidation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class BeanValidationIntegrator implements Integrator {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			BeanValidationIntegrator.class.getName()
	);

	public static final String APPLY_CONSTRAINTS = "hibernate.validator.apply_to_ddl";

	public static final String BV_CHECK_CLASS = "javax.validation.Validation";

	public static final String MODE_PROPERTY = "javax.persistence.validation.mode";

	private static final String ACTIVATOR_CLASS_NAME = "org.hibernate.cfg.beanvalidation.TypeSafeActivator";
	private static final String VALIDATE_SUPPLIED_FACTORY_METHOD_NAME = "validateSuppliedFactory";
	private static final String ACTIVATE_METHOD_NAME = "activate";

	/**
	 * Used to validate the type of an explicitly passed ValidatorFactory instance
	 *
	 * @param object The supposed ValidatorFactory instance
	 */
	@SuppressWarnings("unchecked")
	public static void validateFactory(Object object) {
		try {
			// this direct usage of ClassLoader should be fine since the classes exist in the same jar
			final Class activatorClass = BeanValidationIntegrator.class.getClassLoader().loadClass( ACTIVATOR_CLASS_NAME );
			try {
				final Method validateMethod = activatorClass.getMethod( VALIDATE_SUPPLIED_FACTORY_METHOD_NAME, Object.class );
				try {
					validateMethod.invoke( null, object );
				}
				catch (InvocationTargetException e) {
					if ( e.getTargetException() instanceof HibernateException ) {
						throw (HibernateException) e.getTargetException();
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
			final Metadata metadata,
			final SessionFactoryImplementor sessionFactory,
			final SessionFactoryServiceRegistry serviceRegistry) {
		final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );
		// IMPL NOTE : see the comments on ActivationContext.getValidationModes() as to why this is multi-valued...
		final Set<ValidationMode> modes = ValidationMode.getModes( cfgService.getSettings().get( MODE_PROPERTY ) );
		if ( modes.size() > 1 ) {
			LOG.multipleValidationModes( ValidationMode.loggable( modes ) );
		}
		if ( modes.size() == 1 && modes.contains( ValidationMode.NONE ) ) {
			// we have nothing to do; just return
			return;
		}

		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		// see if the Bean Validation API is available on the classpath
		if ( isBeanValidationApiAvailable( classLoaderService ) ) {
			// and if so, call out to the TypeSafeActivator
			try {
				final Class typeSafeActivatorClass = loadTypeSafeActivatorClass( classLoaderService );
				@SuppressWarnings("unchecked")
				final Method activateMethod = typeSafeActivatorClass.getMethod( ACTIVATE_METHOD_NAME, ActivationContext.class );
				final ActivationContext activationContext = new ActivationContext() {
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
				};

				try {
					activateMethod.invoke( null, activationContext );
				}
				catch (InvocationTargetException e) {
					if ( HibernateException.class.isInstance( e.getTargetException() ) ) {
						throw ( (HibernateException) e.getTargetException() );
					}
					throw new IntegrationException( "Error activating Bean Validation integration", e.getTargetException() );
				}
				catch (Exception e) {
					throw new IntegrationException( "Error activating Bean Validation integration", e );
				}
			}
			catch (NoSuchMethodException e) {
				throw new HibernateException( "Unable to locate TypeSafeActivator#activate method", e );
			}
		}
		else {
			// otherwise check the validation modes
			// todo : in many ways this duplicates thew checks done on the TypeSafeActivator when a ValidatorFactory could not be obtained
			validateMissingBeanValidationApi( modes );
		}
	}

	private boolean isBeanValidationApiAvailable(ClassLoaderService classLoaderService) {
		try {
			classLoaderService.classForName( BV_CHECK_CLASS );
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

	private Class loadTypeSafeActivatorClass(ClassLoaderService classLoaderService) {
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
}
