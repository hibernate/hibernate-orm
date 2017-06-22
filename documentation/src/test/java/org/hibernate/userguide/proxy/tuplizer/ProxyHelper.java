/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.userguide.proxy.tuplizer;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * @author Emmanuel Bernard
 */
//tag::entity-tuplizer-instantiator[]

public class ProxyHelper {

    public static <T> T newProxy(Class<T> targetClass, Serializable id) {
        return ( T ) Proxy.newProxyInstance(
            targetClass.getClassLoader(),
            new Class[] {
                targetClass
            },
            new DataProxyHandler(
                targetClass.getName(),
                id
            )
        );
    }

    public static String extractEntityName(Object object) {
        if ( Proxy.isProxyClass( object.getClass() ) ) {
            InvocationHandler handler = Proxy.getInvocationHandler(
                object
            );
            if ( DataProxyHandler.class.isAssignableFrom( handler.getClass() ) ) {
                DataProxyHandler myHandler = (DataProxyHandler) handler;
                return myHandler.getEntityName();
            }
        }
        return null;
    }
}
//end::entity-tuplizer-instantiator[]

