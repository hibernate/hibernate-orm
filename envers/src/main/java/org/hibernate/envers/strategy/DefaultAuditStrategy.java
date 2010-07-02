package org.hibernate.envers.strategy;

import org.hibernate.Session;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;

import java.io.Serializable;

/**
 * Default strategy is to simply persist the audit data.
 *
 * @author Adam Warski
 * @author Stephanie Pau
 */
public class DefaultAuditStrategy implements AuditStrategy {
    public void perform(Session session, String entityName, AuditConfiguration auditCfg, Serializable id, Object data,
                        Object revision) {
        session.save(auditCfg.getAuditEntCfg().getAuditEntityName(entityName), data);
    }

    public void performCollectionChange(Session session, AuditConfiguration auditCfg,
                                        PersistentCollectionChangeData persistentCollectionChangeData, Object revision) {
        session.save(persistentCollectionChangeData.getEntityName(), persistentCollectionChangeData.getData());
    }
}
