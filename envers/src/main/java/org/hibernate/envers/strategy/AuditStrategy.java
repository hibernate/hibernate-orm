package org.hibernate.envers.strategy;

import org.hibernate.Session;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;

import java.io.Serializable;

/**
 * Behaviours of different audit strategy for populating audit data.
 * 
 * @author Stephanie Pau
 * @author Adam Warski (adam at warski dot org)
 */
public interface AuditStrategy {
    /**
     * Perform the persistence of audited data for regular entities.
     * 
     * @param session Session, which can be used to persist the data.
     * @param entityName Name of the entity, in which the audited change happens
     * @param auditCfg Audit configuration
     * @param id Id of the entity.
     * @param data Audit data to persist
     * @param revision Current revision data
     */
    void perform(Session session, String entityName, AuditConfiguration auditCfg, Serializable id, Object data,
                 Object revision);

    /**
     * Perform the persistence of audited data for collection ("middle") entities.
     *
     * @param session Session, which can be used to persist the data.
     * @param auditCfg Audit configuration
     * @param persistentCollectionChangeData Collection change data to be persisted.
     * @param revision Current revision data
     */
    void performCollectionChange(Session session, AuditConfiguration auditCfg,
                                 PersistentCollectionChangeData persistentCollectionChangeData, Object revision);
}
