package org.hibernate.cfg.beanvalidation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.event.EventListeners;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.util.ReflectHelper;

/**
 * This class has no hard dependency on Bean Validation APIs
 * It must use reflection every time BV is required.
 * @author Emmanuel Bernard
 */
public class BeanValidationActivator {

	private static final String BV_DISCOVERY_CLASS = "javax.validation.Validation";
	private static final String TYPE_SAFE_ACTIVATOR_CLASS = "org.hibernate.cfg.beanvalidation.TypeSafeActivator";
	private static final String TYPE_SAFE_DDL_METHOD = "applyDDL";
	private static final String TYPE_SAFE_ACTIVATOR_METHOD = "activateBeanValidation";
	private static final String MODE_PROPERTY = "javax.persistence.validation.mode";

	public static void activateBeanValidation(EventListeners eventListeners, Properties properties) {
		Set<ValidationMode> modes = ValidationMode.getModes( properties.get( MODE_PROPERTY ) );

		try {
			//load Validation
			ReflectHelper.classForName( BV_DISCOVERY_CLASS, BeanValidationActivator.class );
		}
		catch ( ClassNotFoundException e ) {
			if ( modes.contains( ValidationMode.CALLBACK ) ) {
				throw new HibernateException( "Bean Validation not available in the class path but required in " + MODE_PROPERTY );
			}
			else if (modes.contains( ValidationMode.AUTO ) ) {
				//nothing to activate
				return;
			}
		}

		//de-activate not-null tracking at the core level when Bean Validation
		// is present unless the user really asks for it
		//Note that if BV is not present, the behavior is backward compatible
		if ( properties.getProperty( Environment.CHECK_NULLABILITY ) == null ) {
			properties.setProperty( Environment.CHECK_NULLABILITY, "false" );
		}

		if ( modes.contains( ValidationMode.NONE ) ) return;

		try {
			Class<?> activator = ReflectHelper.classForName( TYPE_SAFE_ACTIVATOR_CLASS, BeanValidationActivator.class );
			Method activateBeanValidation =
					activator.getMethod( TYPE_SAFE_ACTIVATOR_METHOD, EventListeners.class, Properties.class );
			activateBeanValidation.invoke( null, eventListeners, properties );
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

	public static void applyDDL(Collection<PersistentClass> persistentClasses, Properties properties) {
		Set<ValidationMode> modes = ValidationMode.getModes( properties.get( MODE_PROPERTY ) );
		if ( ! ( modes.contains( ValidationMode.DDL ) || modes.contains( ValidationMode.AUTO ) ) ) return;
		try {
			//load Validation
			ReflectHelper.classForName( BV_DISCOVERY_CLASS, BeanValidationActivator.class );
		}
		catch ( ClassNotFoundException e ) {
			if ( modes.contains( ValidationMode.DDL ) ) {
				throw new HibernateException( "Bean Validation not available in the class path but required in " + MODE_PROPERTY );
			}
			else if (modes.contains( ValidationMode.AUTO ) ) {
				//nothing to activate
				return;
			}
		}
		try {
			Class<?> activator = ReflectHelper.classForName( TYPE_SAFE_ACTIVATOR_CLASS, BeanValidationActivator.class );
			Method applyDDL =
					activator.getMethod( TYPE_SAFE_DDL_METHOD, Collection.class, Properties.class );
			applyDDL.invoke( null, persistentClasses, properties );
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
					throw new HibernateException( "Unknown validation mode in " + MODE_PROPERTY + ": " + modeProperty.toString() );
				}
			}
		}
	}
}
