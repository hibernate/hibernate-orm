package org.hibernate.test.dynamicentity;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;

/**
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public class ProxyHelper {

	public static Person newPersonProxy() {
		return newPersonProxy( null );
	}

	public static Person newPersonProxy(Serializable id) {
		return ( Person ) Proxy.newProxyInstance(
				Person.class.getClassLoader(),
		        new Class[] {Person.class},
		        new DataProxyHandler( Person.class.getName(), id )
		);
	}

	public static Customer newCustomerProxy() {
		return newCustomerProxy( null );
	}

	public static Customer newCustomerProxy(Serializable id) {
		return ( Customer ) Proxy.newProxyInstance(
				Customer.class.getClassLoader(),
		        new Class[] {Customer.class},
		        new DataProxyHandler( Customer.class.getName(), id )
		);
	}

	public static Company newCompanyProxy() {
		return newCompanyProxy( null );
	}

	public static Company newCompanyProxy(Serializable id) {
		return ( Company ) Proxy.newProxyInstance(
				Company.class.getClassLoader(),
		        new Class[] {Company.class},
		        new DataProxyHandler( Company.class.getName(), id )
		);
	}

	public static Address newAddressProxy() {
		return newAddressProxy( null );
	}

	public static Address newAddressProxy(Serializable id) {
		return ( Address ) Proxy.newProxyInstance(
				Address.class.getClassLoader(),
		        new Class[] {Address.class},
		        new DataProxyHandler( Address.class.getName(), id )
		);
	}

	public static String extractEntityName(Object object) {
		// Our custom java.lang.reflect.Proxy instances actually bundle
		// their appropriate entity name, so we simply extract it from there
		// if this represents one of our proxies; otherwise, we return null
		if ( Proxy.isProxyClass( object.getClass() ) ) {
			InvocationHandler handler = Proxy.getInvocationHandler( object );
			if ( DataProxyHandler.class.isAssignableFrom( handler.getClass() ) ) {
				DataProxyHandler myHandler = ( DataProxyHandler ) handler;
				return myHandler.getEntityName();
			}
		}
		return null;
	}
}
