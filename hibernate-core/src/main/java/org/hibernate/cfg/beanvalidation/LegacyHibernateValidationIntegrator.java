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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.ResourceBundle;

import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.HibernateLogger;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.event.EventType;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.impl.Integrator;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.event.spi.EventListenerRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class LegacyHibernateValidationIntegrator implements Integrator {
    private static final HibernateLogger LOG = Logger.getMessageLogger( HibernateLogger.class, LegacyHibernateValidationIntegrator.class.getName() );

	public static final String APPLY_CONSTRAINTS = "hibernate.validator.apply_to_ddl";
	public static final String CLASS_VALIDATOR_CLASS = "org.hibernate.validator.ClassValidator";
	public static final String MSG_INTERPOLATOR_CLASS = "org.hibernate.validator.MessageInterpolator";

	public static final String AUTO_REGISTER = "hibernate.validator.autoregister_listeners";
	public static final String LISTENER_CLASS_NAME = "org.hibernate.validator.event.ValidateEventListener";

	// this code mostly all copied and pasted from various spots...

	@Override
	public void integrate(
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		applyRelationalConstraints( configuration, serviceRegistry );
		applyListeners( configuration, serviceRegistry );
	}

	@SuppressWarnings( {"unchecked"})
	private void applyRelationalConstraints(Configuration configuration, SessionFactoryServiceRegistry serviceRegistry) {
		if ( ! ConfigurationHelper.getBoolean( APPLY_CONSTRAINTS, configuration.getProperties(), true ) ){
			LOG.debug( "Skipping application of relational constraints from legacy Hibernate Validator" );
			return;
		}

		Constructor validatorCtr = null;
		Method applyMethod = null;
		try {
			final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
			final Class classValidator = classLoaderService.classForName( CLASS_VALIDATOR_CLASS );
			final Class messageInterpolator = classLoaderService.classForName( MSG_INTERPOLATOR_CLASS );
			validatorCtr = classValidator.getDeclaredConstructor(
					Class.class, ResourceBundle.class, messageInterpolator, Map.class, ReflectionManager.class
			);
			applyMethod = classValidator.getMethod( "apply", PersistentClass.class );
		}
		catch ( NoSuchMethodException e ) {
			throw new AnnotationException( e );
		}
		catch ( Exception e ) {
			LOG.debug( "Legacy Hibernate Validator classes not found, ignoring" );
		}

		if ( applyMethod != null ) {
			Iterable<PersistentClass> persistentClasses = ( (Map<String,PersistentClass>) configuration.createMappings().getClasses() ).values();
			for ( PersistentClass persistentClass : persistentClasses ) {
				// integrate the validate framework
				String className = persistentClass.getClassName();
				if ( StringHelper.isNotEmpty( className ) ) {
					try {
						Object validator = validatorCtr.newInstance(
								ReflectHelper.classForName( className ), null, null, null, configuration.getReflectionManager()
						);
						applyMethod.invoke( validator, persistentClass );
					}
					catch ( Exception e ) {
						LOG.unableToApplyConstraints(className, e);
					}
				}
			}
		}
	}

	private void applyListeners(Configuration configuration, SessionFactoryServiceRegistry serviceRegistry) {
		final boolean registerListeners = ConfigurationHelper.getBoolean( AUTO_REGISTER, configuration.getProperties(), false );
		if ( !registerListeners ) {
			LOG.debug( "Skipping legacy validator auto registration" );
			return;
		}

		final Class listenerClass = loadListenerClass( serviceRegistry );
		if ( listenerClass == null ) {
			LOG.debug( "Skipping legacy validator auto registration - could not locate listener" );
			return;
		}

		final Object validateEventListener;
		try {
			validateEventListener = listenerClass.newInstance();
		}
		catch ( Exception e ) {
			throw new AnnotationException( "Unable to instantiate Validator event listener", e );
		}

		EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		// todo : duplication strategy

		listenerRegistry.appendListeners( EventType.PRE_INSERT, (PreInsertEventListener) validateEventListener );
		listenerRegistry.appendListeners( EventType.PRE_UPDATE, (PreUpdateEventListener) validateEventListener );
	}

	private Class loadListenerClass(SessionFactoryServiceRegistry serviceRegistry) {
		try {
			return serviceRegistry.getService( ClassLoaderService.class ).classForName( LISTENER_CLASS_NAME );
		}
		catch (Exception e) {
			return null;
		}
	}
}
