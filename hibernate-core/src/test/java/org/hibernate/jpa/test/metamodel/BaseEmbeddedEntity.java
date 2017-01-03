
package org.hibernate.jpa.test.metamodel;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseEmbeddedEntity<I extends Serializable, S extends Serializable, E> implements Serializable {
    
    private I id;
    private S data;
    private E entity;

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

    public S getData() {
        return data;
    }

    public void setData(S data) {
        this.data = data;
    }

    public E getEntity() {
        return entity;
    }

    public void setEntity(E entity) {
        this.entity = entity;
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
        final BaseEmbeddedEntity<?, ?, ?> other = (BaseEmbeddedEntity<?, ?, ?>) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }
}
