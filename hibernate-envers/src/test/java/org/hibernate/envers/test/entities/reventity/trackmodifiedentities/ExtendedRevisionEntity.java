package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import javax.persistence.Entity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@RevisionEntity(ExtendedRevisionListener.class)
public class ExtendedRevisionEntity extends DefaultTrackingModifiedEntitiesRevisionEntity {
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
