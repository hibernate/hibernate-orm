package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.envers.enhanced.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.RevisionEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@RevisionEntity(ExtendedRevisionListener.class)
public class ExtendedRevisionEntity extends DefaultTrackingModifiedEntitiesRevisionEntity {
    @Column(name = "USER_COMMENT")
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
