package org.hibernate.cfg.beanvalidation;

import java.util.Map;
import java.util.Properties;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.util.ReflectHelper;
import org.hibernate.HibernateException;
import org.hibernate.AssertionFailure;
import org.hibernate.event.EventListeners;

/**
 * This class has no hard depenmdency on Bean Validation APIs
 * It must uses reflectione very time BV is required.
 * @author Emmanuel Bernard
 */
public class BeanValidationActivator {

	private static final String BV_DISCOVERY_CLASS = "javax.validation.Validation";
	private static final String TYPE_SAFE_ACTIVATOR_CLASS = "org.hibernate.cfg.beanvalidation.TypeSafeActivator";
	private static final String TYPE_SAFE_ACTIVATOR_METHOD = "activateBeanValidation";
	private static final String MODE_PROPERTY = "javax.persistence.validation.mode";

	public static void activateBeanValidation(EventListeners eventListeners, Properties properties) {
		ValidationMode mode = ValidationMode.getMode( properties.get( MODE_PROPERTY ) );
		if (mode == ValidationMode.NONE) return;
		try {
			//load Validation
			ReflectHelper.classForName( BV_DISCOVERY_CLASS, BeanValidationActivator.class );
		}
		catch ( ClassNotFoundException e ) {

			if (mode == ValidationMode.CALLBACK) {
				throw new HibernateException( "Bean Validation not available in the class path but required in " + MODE_PROPERTY );
			}
			else if (mode == ValidationMode.AUTO) {
				//nothing to activate
				return;
			}
			else {
				throw new AssertionFailure( "Unexpected ValidationMode: " + mode );
			}
		}
		try {
			Class<?> activator = ReflectHelper.classForName( TYPE_SAFE_ACTIVATOR_CLASS, BeanValidationActivator.class );
			Method buildDefaultValidatorFactory =
					activator.getMethod( TYPE_SAFE_ACTIVATOR_METHOD, EventListeners.class, Properties.class );
			buildDefaultValidatorFactory.invoke( null, eventListeners, properties );
		}
		catch ( NoSuchMethodException e ) {
			throw new HibernateException( "Unable to get the default Bean Validation factory", e);
		}
		catch ( IllegalAccessException e ) {
			throw new HibernateException( "Unable to get the default Bean Validation factory", e);
		}
		catch ( InvocationTargetException e ) {
			throw new HibernateException( "Unable to get the default Bean Validation factory", e);
		}
		catch ( ClassNotFoundException e ) {
			throw new HibernateException( "Unable to get the default Bean Validation factory", e);
		}
	}

	private static enum ValidationMode {
		AUTO,
		CALLBACK,
		NONE;

		public static ValidationMode getMode(Object modeProperty) {
			if (modeProperty == null) {
				return AUTO;
			}
			else {
				try {
					return valueOf( modeProperty.toString().toUpperCase() );
				}
				catch ( IllegalArgumentException e ) {
					throw new HibernateException( "Unknown validation mode in " + MODE_PROPERTY + ": " + modeProperty.toString() );
				}
			}
		}
	}
}
