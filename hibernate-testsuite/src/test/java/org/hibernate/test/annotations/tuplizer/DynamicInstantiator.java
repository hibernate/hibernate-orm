//$Id$
package org.hibernate.test.annotations.tuplizer;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;

import org.hibernate.tuple.Instantiator;
import org.hibernate.util.ReflectHelper;
import org.hibernate.HibernateException;

/**
 * @author Emmanuel Bernard
 */
public class DynamicInstantiator implements Instantiator {
	private final String entityName;

	public DynamicInstantiator(String entityName) {
		this.entityName = entityName;
	}

	public Object instantiate(Serializable id) {
		if ( Cuisine.class.getName().equals( entityName ) ) {
			return ProxyHelper.newCuisineProxy( id );
		}
		if ( Country.class.getName().equals( entityName ) ) {
			return ProxyHelper.newCountryProxy( id );
		}
		else {
			throw new IllegalArgumentException( "unknown entity for instantiation [" + entityName + "]" );
		}
	}

	public Object instantiate() {
		return instantiate( null );
	}

	public boolean isInstance(Object object) {
		String resolvedEntityName = null;
		if ( Proxy.isProxyClass( object.getClass() ) ) {
			InvocationHandler handler = Proxy.getInvocationHandler( object );
			if ( DataProxyHandler.class.isAssignableFrom( handler.getClass() ) ) {
				DataProxyHandler myHandler = ( DataProxyHandler ) handler;
				resolvedEntityName = myHandler.getEntityName();
			}
		}
		try {
			return ReflectHelper.classForName( entityName ).isInstance( object );
		}
		catch( Throwable t ) {
			throw new HibernateException( "could not get handle to entity-name as interface : " + t );
		}

//		return entityName.equals( resolvedEntityName );
	}
}
