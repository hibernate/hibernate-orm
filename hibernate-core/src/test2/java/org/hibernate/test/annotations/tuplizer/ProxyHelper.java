/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.tuplizer;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * @author Emmanuel Bernard
 */
public class ProxyHelper {

	public static Country newPersonProxy() {
		return newCountryProxy( null );
	}

	public static Country newCountryProxy(Serializable id) {
		return ( Country ) Proxy.newProxyInstance(
				Country.class.getClassLoader(),
		        new Class[] {Country.class},
		        new DataProxyHandler( Country.class.getName(), id )
		);
	}

	public static Cuisine newCustomerProxy() {
		return newCuisineProxy( null );
	}

	public static Cuisine newCuisineProxy(Serializable id) {
		return ( Cuisine ) Proxy.newProxyInstance(
				Cuisine.class.getClassLoader(),
		        new Class[] {Cuisine.class},
		        new DataProxyHandler( Cuisine.class.getName(), id )
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
