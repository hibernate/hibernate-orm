
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.MappedSuperclass;

/**
 * @author Christian Beikov
 */
@MappedSuperclass
public abstract class BaseEmbeddedEntity<I extends Serializable> implements Serializable {
    
    private I id;

    public BaseEmbeddedEntity() {
    }

    public BaseEmbeddedEntity(I id) {
        this.id = id;
    }

    @EmbeddedId
    public I getId() {
        return id;
    }

    public void setId(I id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BaseEmbeddedEntity<?> other = (BaseEmbeddedEntity<?>) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }
}
