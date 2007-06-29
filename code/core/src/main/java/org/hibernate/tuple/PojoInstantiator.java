//$Id: PojoInstantiator.java 9210 2006-02-03 22:15:19Z steveebersole $
package org.hibernate.tuple;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.InstantiationException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.bytecode.ReflectionOptimizer;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Component;
import org.hibernate.util.ReflectHelper;

/**
 * Defines a POJO-based instantiator for use from the tuplizers.
 */
public class PojoInstantiator implements Instantiator, Serializable {

	private static final Log log = LogFactory.getLog(PojoInstantiator.class);

	private transient Constructor constructor;

	private final Class mappedClass;
	private final transient ReflectionOptimizer.InstantiationOptimizer optimizer;
	private final boolean embeddedIdentifier;
	private final Class proxyInterface;

	public PojoInstantiator(Component component, ReflectionOptimizer.InstantiationOptimizer optimizer) {
		this.mappedClass = component.getComponentClass();
		this.optimizer = optimizer;

		this.proxyInterface = null;
		this.embeddedIdentifier = false;

		try {
			constructor = ReflectHelper.getDefaultConstructor(mappedClass);
		}
		catch ( PropertyNotFoundException pnfe ) {
			log.info(
			        "no default (no-argument) constructor for class: " +
					mappedClass.getName() +
					" (class must be instantiated by Interceptor)"
			);
			constructor = null;
		}
	}

	public PojoInstantiator(PersistentClass persistentClass, ReflectionOptimizer.InstantiationOptimizer optimizer) {
		this.mappedClass = persistentClass.getMappedClass();
		this.proxyInterface = persistentClass.getProxyInterface();
		this.embeddedIdentifier = persistentClass.hasEmbeddedIdentifier();
		this.optimizer = optimizer;

		try {
			constructor = ReflectHelper.getDefaultConstructor( mappedClass );
		}
		catch ( PropertyNotFoundException pnfe ) {
			log.info(
			        "no default (no-argument) constructor for class: " +
					mappedClass.getName() +
					" (class must be instantiated by Interceptor)"
			);
			constructor = null;
		}
	}

	private void readObject(java.io.ObjectInputStream stream)
	throws ClassNotFoundException, IOException {
		stream.defaultReadObject();
		constructor = ReflectHelper.getDefaultConstructor(mappedClass);
	}

	public Object instantiate() {
		if ( ReflectHelper.isAbstractClass(mappedClass) ) {
			throw new InstantiationException( "Cannot instantiate abstract class or interface: ", mappedClass );
		}
		else if ( optimizer != null ) {
			return optimizer.newInstance();
		}
		else if ( constructor == null ) {
			throw new InstantiationException( "No default constructor for entity: ", mappedClass );
		}
		else {
			try {
				return constructor.newInstance( null );
			}
			catch ( Exception e ) {
				throw new InstantiationException( "Could not instantiate entity: ", mappedClass, e );
			}
		}
	}
	
	public Object instantiate(Serializable id) {
		final boolean useEmbeddedIdentifierInstanceAsEntity = embeddedIdentifier && 
				id != null && 
				id.getClass().equals(mappedClass);
		return useEmbeddedIdentifierInstanceAsEntity ? id : instantiate();
	}

	public boolean isInstance(Object object) {
		return mappedClass.isInstance(object) || 
				( proxyInterface!=null && proxyInterface.isInstance(object) ); //this one needed only for guessEntityMode()
	}
}