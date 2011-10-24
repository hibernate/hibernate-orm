/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg.beanvalidation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class BeanValidationIntegrator implements Integrator {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, BeanValidationIntegrator.class.getName());

	public static final String APPLY_CONSTRAINTS = "hibernate.validator.apply_to_ddl";

	public static final String BV_CHECK_CLASS = "javax.validation.Validation";

	public static final String MODE_PROPERTY = "javax.persistence.validation.mode";

	private static final String ACTIVATOR_CLASS = "org.hibernate.cfg.beanvalidation.TypeSafeActivator";
	private static final String DDL_METHOD = "applyDDL";
	private static final String ACTIVATE_METHOD = "activateBeanValidation";
	private static final String VALIDATE_METHOD = "validateFactory";

	public static void validateFactory(Object object) {
		try {
			final Class activatorClass = BeanValidationIntegrator.class.getClassLoader().loadClass( ACTIVATOR_CLASS );
			try {
				final Method validateMethod = activatorClass.getMethod( VALIDATE_METHOD, Object.class );
				if ( ! validateMethod.isAccessible() ) {
					validateMethod.setAccessible( true );
				}
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
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		// determine requested validation modes.
		final Set<ValidationMode> modes = ValidationMode.getModes( configuration.getProperties().get( MODE_PROPERTY ) );

		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
        Dialect dialect = serviceRegistry.getService( JdbcServices.class ).getDialect();
		// try to locate a BV class to see if it is available on the classpath
		boolean isBeanValidationAvailable;
		try {
			classLoaderService.classForName( BV_CHECK_CLASS );
			isBeanValidationAvailable = true;
		}
		catch ( Exception e ) {
			isBeanValidationAvailable = false;
		}

		// locate the type safe activator class
		final Class typeSafeActivatorClass = loadTypeSafeActivatorClass( serviceRegistry );

		// todo : if this works out, probably better to simply alter TypeSafeActivator into a single method...
		applyRelationalConstraints(
				modes,
				isBeanValidationAvailable,
				typeSafeActivatorClass,
				configuration,
                dialect

		);
		applyHibernateListeners(
				modes,
				isBeanValidationAvailable,
				typeSafeActivatorClass,
				configuration,
				sessionFactory,
				serviceRegistry
		);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.integrator.spi.Integrator#integrate(org.hibernate.metamodel.source.MetadataImplementor, org.hibernate.engine.spi.SessionFactoryImplementor, org.hibernate.service.spi.SessionFactoryServiceRegistry)
	 */
	@Override
	public void integrate( MetadataImplementor metadata,
	                       SessionFactoryImplementor sessionFactory,
	                       SessionFactoryServiceRegistry serviceRegistry ) {
//	    Properties props = sessionFactory.getProperties();
//        final Set<ValidationMode> modes = ValidationMode.getModes(props.get(MODE_PROPERTY));
//        final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
//        // try to locate a BV class to see if it is available on the classpath
//        boolean isBeanValidationAvailable;
//        try {
//            classLoaderService.classForName( BV_CHECK_CLASS );
//            isBeanValidationAvailable = true;
//        } catch (Exception error) {
//            isBeanValidationAvailable = false;
//        }
//        // locate the type safe activator class
//        final Class typeSafeActivatorClass = loadTypeSafeActivatorClass(serviceRegistry);
//        // todo : if this works out, probably better to simply alter TypeSafeActivator into a single method...
//        applyRelationalConstraints(modes, isBeanValidationAvailable, typeSafeActivatorClass, props, metadata);
//        applyHibernateListeners(modes, isBeanValidationAvailable, typeSafeActivatorClass, sessionFactory, serviceRegistry);
	}

	private Class loadTypeSafeActivatorClass(SessionFactoryServiceRegistry serviceRegistry) {
		try {
			return serviceRegistry.getService( ClassLoaderService.class ).classForName( ACTIVATOR_CLASS );
		}
		catch (Exception e) {
			return null;
		}
	}

	private void applyRelationalConstraints(
			Set<ValidationMode> modes,
			boolean beanValidationAvailable,
			Class typeSafeActivatorClass,
			Configuration configuration,
            Dialect dialect) {
		if ( ! ConfigurationHelper.getBoolean( APPLY_CONSTRAINTS, configuration.getProperties(), true ) ){
			LOG.debug( "Skipping application of relational constraints from legacy Hibernate Validator" );
			return;
		}

		if ( ! ( modes.contains( ValidationMode.DDL ) || modes.contains( ValidationMode.AUTO ) ) ) {
			return;
		}

		if ( ! beanValidationAvailable ) {
			if ( modes.contains( ValidationMode.DDL ) ) {
				throw new HibernateException( "Bean Validation not available in the class path but required in " + MODE_PROPERTY );
			}
			else if (modes.contains( ValidationMode.AUTO ) ) {
				//nothing to activate
				return;
			}
		}

		try {
			Method applyDDLMethod = typeSafeActivatorClass.getMethod( DDL_METHOD, Collection.class, Properties.class, Dialect.class );
			try {
				applyDDLMethod.invoke(
						null,
						configuration.createMappings().getClasses().values(),
						configuration.getProperties(),
                        dialect
				);
			}
			catch (HibernateException e) {
				throw e;
			}
			catch (Exception e) {
				throw new HibernateException( "Error applying BeanValidation relational constraints", e );
			}
		}
		catch (HibernateException e) {
			throw e;
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to locate TypeSafeActivator#applyDDL method", e );
		}
	}

//    private void applyRelationalConstraints( Set<ValidationMode> modes,
//                                             boolean beanValidationAvailable,
//                                             Class typeSafeActivatorClass,
//                                             Properties properties,
//                                             MetadataImplementor metadata ) {
//        if (!ConfigurationHelper.getBoolean(APPLY_CONSTRAINTS, properties, true)){
//            LOG.debug("Skipping application of relational constraints from legacy Hibernate Validator");
//            return;
//        }
//        if (!(modes.contains(ValidationMode.DDL) || modes.contains(ValidationMode.AUTO))) return;
//        if (!beanValidationAvailable) {
//            if (modes.contains(ValidationMode.DDL))
//                throw new HibernateException("Bean Validation not available in the class path but required in " + MODE_PROPERTY);
//            if(modes.contains(ValidationMode.AUTO)) return; //nothing to activate
//        }
//        try {
//            Method applyDDLMethod = typeSafeActivatorClass.getMethod(DDL_METHOD, Iterable.class, Properties.class, ClassLoaderService.class);
//            try {
//                applyDDLMethod.invoke(null, metadata.getEntityBindings(), properties,
//                                      metadata.getServiceRegistry().getService(ClassLoaderService.class));
//            } catch (HibernateException error) {
//                throw error;
//            } catch (Exception error) {
//                throw new HibernateException("Error applying BeanValidation relational constraints", error);
//            }
//        } catch (HibernateException error) {
//            throw error;
//        } catch (Exception error) {
//            throw new HibernateException("Unable to locate TypeSafeActivator#applyDDL method", error);
//        }
//    }

	private void applyHibernateListeners(
			Set<ValidationMode> modes,
			boolean beanValidationAvailable,
			Class typeSafeActivatorClass,
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		// de-activate not-null tracking at the core level when Bean Validation is present unless the user explicitly
		// asks for it
		if ( configuration.getProperty( Environment.CHECK_NULLABILITY ) == null ) {
			sessionFactory.getSettings().setCheckNullability( false );
		}

		if ( ! ( modes.contains( ValidationMode.CALLBACK ) || modes.contains( ValidationMode.AUTO ) ) ) {
			return;
		}

		if ( ! beanValidationAvailable ) {
			if ( modes.contains( ValidationMode.CALLBACK ) ) {
				throw new HibernateException( "Bean Validation not available in the class path but required in " + MODE_PROPERTY );
			}
			else if (modes.contains( ValidationMode.AUTO ) ) {
				//nothing to activate
				return;
			}
		}

		try {
			Method activateMethod = typeSafeActivatorClass.getMethod( ACTIVATE_METHOD, EventListenerRegistry.class, Configuration.class );
			try {
				activateMethod.invoke(
						null,
						serviceRegistry.getService( EventListenerRegistry.class ),
						configuration
				);
			}
			catch (HibernateException e) {
				throw e;
			}
			catch (Exception e) {
				throw new HibernateException( "Error applying BeanValidation relational constraints", e );
			}
		}
		catch (HibernateException e) {
			throw e;
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to locate TypeSafeActivator#applyDDL method", e );
		}
	}

//    private void applyHibernateListeners( Set<ValidationMode> modes,
//                                          boolean beanValidationAvailable,
//                                          Class typeSafeActivatorClass,
//                                          SessionFactoryImplementor sessionFactory,
//                                          SessionFactoryServiceRegistry serviceRegistry ) {
//        // de-activate not-null tracking at the core level when Bean Validation is present unless the user explicitly
//        // asks for it
//        if (sessionFactory.getProperties().getProperty(Environment.CHECK_NULLABILITY) == null)
//            sessionFactory.getSettings().setCheckNullability( false );
//        if (!(modes.contains( ValidationMode.CALLBACK) || modes.contains(ValidationMode.AUTO))) return;
//        if (!beanValidationAvailable) {
//            if (modes.contains(ValidationMode.CALLBACK))
//                throw new HibernateException("Bean Validation not available in the class path but required in " + MODE_PROPERTY);
//            if (modes.contains(ValidationMode.AUTO)) return; //nothing to activate
//        }
//        try {
//            Method activateMethod = typeSafeActivatorClass.getMethod(ACTIVATE_METHOD, EventListenerRegistry.class);
//            try {
//                activateMethod.invoke(null, serviceRegistry.getService(EventListenerRegistry.class));
//            }
//            catch (HibernateException e) {
//                throw e;
//            }
//            catch (Exception e) {
//                throw new HibernateException( "Error applying BeanValidation relational constraints", e );
//            }
//        }
//        catch (HibernateException e) {
//            throw e;
//        }
//        catch (Exception e) {
//            throw new HibernateException( "Unable to locate TypeSafeActivator#applyDDL method", e );
//        }
//    }

	// Because the javax validation classes might not be on the runtime classpath
	private static enum ValidationMode {
		AUTO,
		CALLBACK,
		NONE,
		DDL;

		public static Set<ValidationMode> getModes(Object modeProperty) {
			Set<ValidationMode> modes = new HashSet<ValidationMode>(3);
			if (modeProperty == null) {
				modes.add(ValidationMode.AUTO);
			}
			else {
				final String[] modesInString = modeProperty.toString().split( "," );
				for ( String modeInString : modesInString ) {
					modes.add( getMode(modeInString) );
				}
			}
			if ( modes.size() > 1 && ( modes.contains( ValidationMode.AUTO ) || modes.contains( ValidationMode.NONE ) ) ) {
				StringBuilder message = new StringBuilder( "Incompatible validation modes mixed: " );
				for (ValidationMode mode : modes) {
					message.append( mode ).append( ", " );
				}
				throw new HibernateException( message.substring( 0, message.length() - 2 ) );
			}
			return modes;
		}

		private static ValidationMode getMode(String modeProperty) {
			if (modeProperty == null || modeProperty.length() == 0) {
				return AUTO;
			}
			else {
				try {
					return valueOf( modeProperty.trim().toUpperCase() );
				}
				catch ( IllegalArgumentException e ) {
					throw new HibernateException( "Unknown validation mode in " + MODE_PROPERTY + ": " + modeProperty );
				}
			}
		}
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// nothing to do here afaik
	}
}
