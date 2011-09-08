package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionType;

import java.io.Serializable;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class CustomTrackingRevisionListener implements EntityTrackingRevisionListener {
    public void entityChanged(Class entityClass, String entityName, Serializable entityId, RevisionType revisionType,
                              Object revisionEntity) {
        ((CustomTrackingRevisionEntity)revisionEntity).addModifiedEntityType(entityClass.getName());
    }

    public void newRevision(Object revisionEntity) {
    }
}
