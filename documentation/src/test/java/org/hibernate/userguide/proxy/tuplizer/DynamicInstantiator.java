/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.userguide.proxy.tuplizer;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.tuple.Instantiator;

/**
 * @author Emmanuel Bernard
 */
//tag::entity-tuplizer-instantiator[]

public class DynamicInstantiator
        implements Instantiator {

    private final Class targetClass;

    public DynamicInstantiator(String targetClassName) {
        try {
            this.targetClass = Class.forName( targetClassName );
        }
        catch (ClassNotFoundException e) {
            throw new HibernateException( e );
        }
    }

    public Object instantiate(Serializable id) {
        return ProxyHelper.newProxy( targetClass, id );
    }

    public Object instantiate() {
        return instantiate( null );
    }

    public boolean isInstance(Object object) {
        try {
            return targetClass.isInstance( object );
        }
        catch( Throwable t ) {
            throw new HibernateException(
                "could not get handle to entity as interface : " + t
            );
        }
    }
}
//end::entity-tuplizer-instantiator[]
