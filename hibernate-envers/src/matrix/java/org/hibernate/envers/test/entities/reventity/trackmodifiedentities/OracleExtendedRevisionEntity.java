package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.envers.RevisionEntity;

import javax.persistence.Entity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@RevisionEntity(OracleExtendedRevisionListener.class)
public class OracleExtendedRevisionEntity extends AbstractOracleTrackingModifiedEntitiesRevisionEntity {
    private String userComment;

    public String getUserComment() {
        return userComment;
    }

    public void setUserComment(String userComment) {
        this.userComment = userComment;
    }
}
