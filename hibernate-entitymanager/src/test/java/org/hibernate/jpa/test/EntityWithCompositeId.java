package org.hibernate.jpa.test;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 *
 */
@Entity
public class EntityWithCompositeId implements Serializable {

    @EmbeddedId
    private CompositeId id;
    private String description;

    public CompositeId getId() {
        return id;
    }

    public void setId(CompositeId id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
