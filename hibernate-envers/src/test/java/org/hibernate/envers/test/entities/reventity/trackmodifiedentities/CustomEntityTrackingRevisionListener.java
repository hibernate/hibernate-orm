package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.envers.EntityTrackingRevisionListener;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class CustomEntityTrackingRevisionListener implements EntityTrackingRevisionListener {
    @Override
    public void addEntityToRevision(String entityName, Object revisionEntity) {
        ((CustomTrackingRevisionEntity)revisionEntity).addModifiedEntityName(entityName);
    }

    @Override
    public void removeEntityFromRevision(String entityName, Object revisionEntity) {
        ((CustomTrackingRevisionEntity)revisionEntity).removeModifiedEntityName(entityName);
    }

    @Override
    public void newRevision(Object revisionEntity) {
    }
}
