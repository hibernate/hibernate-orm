package org.hibernate.userguide.events;

import java.sql.Timestamp;
import javax.persistence.MappedSuperclass;

/**
 * @author Vlad Mihalcea
 */
//tag::events-default-listener-mapping-example[]
@MappedSuperclass
public abstract class BaseEntity {

    private Timestamp createdOn;

    private Timestamp updatedOn;

    public Timestamp getCreatedOn() {
        return createdOn;
    }

    void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }

    public Timestamp getUpdatedOn() {
        return updatedOn;
    }

    void setUpdatedOn(Timestamp updatedOn) {
        this.updatedOn = updatedOn;
    }
}
//end::events-default-listener-mapping-example[]

