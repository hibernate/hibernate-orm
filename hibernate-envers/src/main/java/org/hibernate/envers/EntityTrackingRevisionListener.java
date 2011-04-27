package org.hibernate.envers;

/**
 * Extension of standard {@link RevisionListener} that notifies whenever tracking of each audited entity
 * instance is started or stopped within the current revision boundaries.
 * @see RevisionListener
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public interface EntityTrackingRevisionListener extends RevisionListener {
    /**
     * Called after audited entity data has been persisted.
     * @param entityName Name of the audited entity.
     * @param revisionEntity An instance of the entity annotated with {@link RevisionEntity}.
     */
    void addEntityToRevision(String entityName, Object revisionEntity);

    /**
     * Called when persistence of a given audited entity snapshot has been already performed in a previous unit of work.
     * @param entityName Name of the audited entity.
     * @param revisionEntity An instance of the entity annotated with {@link RevisionEntity}.
     */
    void removeEntityFromRevision(String entityName, Object revisionEntity);
}
