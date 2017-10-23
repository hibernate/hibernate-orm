/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.naturalid.cid;

import javax.persistence.Embeddable;

/**
 * @author Donnchadh O Donnabhain
 */

@Embeddable
public class AId implements java.io.Serializable {
    private final int id;

    protected AId() {
        this.id = 0;
    }
    
    public AId(int id) {
        this.id = id;
    }
    public int intValue() {
        return id;
    }
    @Override
    public int hashCode() {
        return id;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AId other = (AId) obj;
        if (other != null && id != other.id)
            return false;
        return true;
    }
}

