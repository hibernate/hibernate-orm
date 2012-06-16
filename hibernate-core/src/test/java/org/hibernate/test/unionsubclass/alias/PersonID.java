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

/**
 * 
 * @author Strong Liu <stliu@redhat.com>
 */
public class PersonID implements Serializable {
    private Long num;
    private String name;

    public Long getNum() {
        return num;
    }

    public void setNum( Long num ) {
        this.num = num;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        final PersonID other = (PersonID) obj;
        if ( name == null ) {
            if ( other.name != null )
                return false;

        } else if ( !name.equals( other.name ) ) {
            return false;
        }
        if ( num == null ) {
            if ( other.num != null )
                return false;

        } else if ( !num.equals( other.num ) ) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        if ( name != null ) {
            result += name.hashCode();
        }
        result *= PRIME;
        if ( num != null ) {
            result += num.hashCode();
        }
        return result;
    }

    public String toString() {
        return name + " | " + num;
    }

}
