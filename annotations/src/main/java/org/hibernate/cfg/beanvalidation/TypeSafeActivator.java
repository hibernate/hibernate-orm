package org.hibernate.cfg.beanvalidation;

import java.util.Map;
import java.util.Arrays;
import java.util.Properties;
import javax.validation.ValidatorFactory;
import javax.validation.Validation;

import org.hibernate.HibernateException;
import org.hibernate.event.EventListeners;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.event.PreDeleteEventListener;

/**
 * @author Emmanuel Bernard
 */
class TypeSafeActivator {

	private static final String FACTORY_PROPERTY = "javax.persistence.validation.factory";

	public static void activateBeanValidation(EventListeners eventListeners, Properties properties) {
		ValidatorFactory factory = getValidatorFactory( properties );
		BeanValidationEventListener beanValidationEventListener = new BeanValidationEventListener( factory, properties );

		{
			PreInsertEventListener[] listeners = eventListeners.getPreInsertEventListeners();
			int length = listeners.length + 1;
			PreInsertEventListener[] newListeners = new PreInsertEventListener[length];
			System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
			newListeners[length - 1] = beanValidationEventListener;
			eventListeners.setPreInsertEventListeners( newListeners );
		}

		{
			PreUpdateEventListener[] listeners = eventListeners.getPreUpdateEventListeners();
			int length = listeners.length + 1;
			PreUpdateEventListener[] newListeners = new PreUpdateEventListener[length];
			System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
			newListeners[length - 1] = beanValidationEventListener;
			eventListeners.setPreUpdateEventListeners( newListeners );
		}

		{
			PreDeleteEventListener[] listeners = eventListeners.getPreDeleteEventListeners();
			int length = listeners.length + 1;
			PreDeleteEventListener[] newListeners = new PreDeleteEventListener[length];
			System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
			newListeners[length - 1] = beanValidationEventListener;
			eventListeners.setPreDeleteEventListeners( newListeners );
		}
	}

	static ValidatorFactory getValidatorFactory(Map<Object, Object> properties) {
		ValidatorFactory factory = null;
		if ( properties != null ) {
			Object unsafeProperty = properties.get( FACTORY_PROPERTY );
			if (unsafeProperty != null) {
				try {
					factory = ValidatorFactory.class.cast( unsafeProperty );
				}
				catch ( ClassCastException e ) {
					throw new HibernateException( "Property " + FACTORY_PROPERTY
							+ " should containt an object of type " + ValidatorFactory.class.getName() );
				}
			}
		}
		if (factory == null) {
			try {
				factory = Validation.buildDefaultValidatorFactory();
			}
			catch ( Exception e ) {
				throw new HibernateException( "Unable to build the default ValidatorFactory", e);
			}
		}
		return factory;
	}

}
