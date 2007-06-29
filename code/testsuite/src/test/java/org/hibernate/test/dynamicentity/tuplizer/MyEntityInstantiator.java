package org.hibernate.test.dynamicentity.tuplizer;

import org.hibernate.tuple.Instantiator;
import org.hibernate.test.dynamicentity.Customer;
import org.hibernate.test.dynamicentity.ProxyHelper;
import org.hibernate.test.dynamicentity.Company;
import org.hibernate.test.dynamicentity.DataProxyHandler;
import org.hibernate.test.dynamicentity.Address;
import org.hibernate.test.dynamicentity.Person;
import org.hibernate.util.ReflectHelper;
import org.hibernate.HibernateException;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;

/**
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public class MyEntityInstantiator implements Instantiator {
	private final String entityName;

	public MyEntityInstantiator(String entityName) {
		this.entityName = entityName;
	}

	public Object instantiate(Serializable id) {
		if ( Person.class.getName().equals( entityName ) ) {
			return ProxyHelper.newPersonProxy( id );
		}
		if ( Customer.class.getName().equals( entityName ) ) {
			return ProxyHelper.newCustomerProxy( id );
		}
		else if ( Company.class.getName().equals( entityName ) ) {
			return ProxyHelper.newCompanyProxy( id );
		}
		else if ( Address.class.getName().equals( entityName ) ) {
			return ProxyHelper.newAddressProxy( id );
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
