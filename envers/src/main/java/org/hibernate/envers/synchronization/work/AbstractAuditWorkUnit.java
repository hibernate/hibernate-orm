/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.synchronization.work;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.configuration.AuditEntitiesConfiguration;

import org.hibernate.Session;
import org.hibernate.engine.SessionImplementor;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractAuditWorkUnit implements AuditWorkUnit {
	protected final SessionImplementor sessionImplementor;
    protected final AuditConfiguration verCfg;
    protected final Serializable id;
    protected final String entityName;

    private Object performedData;

    protected AbstractAuditWorkUnit(SessionImplementor sessionImplementor, String entityName, AuditConfiguration verCfg,
									Serializable id) {
		this.sessionImplementor = sessionImplementor;
        this.verCfg = verCfg;
        this.id = id;
        this.entityName = entityName;
    }

    protected void fillDataWithId(Map<String, Object> data, Object revision, RevisionType revisionType) {
        AuditEntitiesConfiguration entitiesCfg = verCfg.getAuditEntCfg();

        Map<String, Object> originalId = new HashMap<String, Object>();
        originalId.put(entitiesCfg.getRevisionFieldName(), revision);

        verCfg.getEntCfg().get(getEntityName()).getIdMapper().mapToMapFromId(originalId, id);
        data.put(entitiesCfg.getRevisionTypePropName(), revisionType);
        data.put(entitiesCfg.getOriginalIdPropName(), originalId);
    }

    public void perform(Session session, Object revisionData) {
        Map<String, Object> data = generateData(revisionData);

        session.save(verCfg.getAuditEntCfg().getAuditEntityName(getEntityName()), data);

        setPerformed(data);
    }

    public Object getEntityId() {
        return id;
    }

    public boolean isPerformed() {
        return performedData != null;
    }

    public String getEntityName() {
        return entityName;
    }

    protected void setPerformed(Object performedData) {
        this.performedData = performedData;
    }

    public void undo(Session session) {
        if (isPerformed()) {
            session.delete(verCfg.getAuditEntCfg().getAuditEntityName(getEntityName()), performedData);
            session.flush();
        }
    }
}
