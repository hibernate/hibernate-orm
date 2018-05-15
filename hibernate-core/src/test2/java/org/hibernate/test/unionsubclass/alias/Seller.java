/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.unionsubclass.alias;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author Strong Liu <stliu@redhat.com>
 */
public class Seller implements Serializable {
    private PersonID id;
    private Set buyers = new HashSet();

    public PersonID getId() {
        return id;
    }

    public void setId( PersonID id ) {
        this.id = id;
    }

    public Set getBuyers() {
        return buyers;
    }

    public void setBuyers( Set buyers ) {
        this.buyers = buyers;
    }

    public boolean equals( Object obj ) {
        if ( obj == null )
            return false;
        if ( obj == this )
            return true;
        if ( !( obj instanceof Seller ) )
            return false;

        return ( (Seller) obj ).getId().equals( getId() );
    }

    public int hashCode() {
        return id.hashCode();
    }

}
