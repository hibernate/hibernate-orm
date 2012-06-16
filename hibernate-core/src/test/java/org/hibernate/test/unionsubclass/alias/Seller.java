/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
