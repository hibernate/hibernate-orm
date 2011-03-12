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

import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ModWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
    private final Map<String, Object> data;
    private final boolean changes;        

    public ModWorkUnit(SessionImplementor sessionImplementor, String entityName, AuditConfiguration verCfg, 
					   Serializable id, EntityPersister entityPersister, Object[] newState, Object[] oldState) {
        super(sessionImplementor, entityName, verCfg, id);

        data = new HashMap<String, Object>();
        changes = verCfg.getEntCfg().get(getEntityName()).getPropertyMapper().map(sessionImplementor, data,
				entityPersister.getPropertyNames(), newState, oldState);
    }

    public boolean containsWork() {
        return changes;
    }

    public Map<String, Object> generateData(Object revisionData) {
        fillDataWithId(data, revisionData, RevisionType.MOD);

        return data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public AuditWorkUnit merge(AddWorkUnit second) {
        return this;
    }

    public AuditWorkUnit merge(ModWorkUnit second) {
        return second;
    }

    public AuditWorkUnit merge(DelWorkUnit second) {
        return second;
    }

    public AuditWorkUnit merge(CollectionChangeWorkUnit second) {
        return this;
    }

    public AuditWorkUnit merge(FakeBidirectionalRelationWorkUnit second) {
        return FakeBidirectionalRelationWorkUnit.merge(second, this, second.getNestedWorkUnit());
    }

    public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {
        return first.merge(this);
    }
}