package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.DefaultTrackingModifiedTypesRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import javax.persistence.Entity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@RevisionEntity(ExtendedRevisionListener.class)
public class ExtendedRevisionEntity extends DefaultTrackingModifiedTypesRevisionEntity {
    private String commnent;

    public String getCommnent() {
        return commnent;
    }

    public void setCommnent(String commnent) {
        this.commnent = commnent;
    }
}
