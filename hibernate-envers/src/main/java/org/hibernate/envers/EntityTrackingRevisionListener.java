package org.hibernate.envers;

import java.io.Serializable;

/**
 * Extension of standard {@link RevisionListener} that notifies whenever an entity instance has been
 * added, modified or removed within current revision boundaries.
 * @see RevisionListener
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public interface EntityTrackingRevisionListener extends RevisionListener {
    /**
     * Called after audited entity data has been persisted.
     * @param entityClass Audited entity class.
     * @param entityName Name of the audited entity. May be useful when Java class is mapped multiple times,
     *                   potentially to different tables. 
     * @param entityId Identifier of modified entity.
     * @param revisionType Modification type (addition, update or removal).
     * @param revisionEntity An instance of the entity annotated with {@link RevisionEntity}.
     */
    void entityChanged(Class entityClass, String entityName, Serializable entityId, RevisionType revisionType,
                       Object revisionEntity);
}
