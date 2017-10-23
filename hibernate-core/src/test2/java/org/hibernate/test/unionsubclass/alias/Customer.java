/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.unionsubclass.alias;
import java.io.Serializable;

/**
 * 
 * @author Strong Liu <stliu@redhat.com>
 */
public abstract class Customer implements Serializable {
    private PersonID id;

    public PersonID getId() {
        return id;
    }

    public void setId( PersonID id ) {
        this.id = id;
    }

    public boolean equals( Object obj ) {
        if ( obj == null )
            return false;
        if ( obj == this )
            return true;
        if ( !( obj instanceof Customer ) )
            return false;
        return ( (Customer) obj ).getId().equals( getId() );
    }

    public int hashCode() {
        return id.hashCode();
    }

}
