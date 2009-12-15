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
public class AddWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
    private final Map<String, Object> data;

    public AddWorkUnit(SessionImplementor sessionImplementor, String entityName, AuditConfiguration verCfg,
					   Serializable id, EntityPersister entityPersister, Object[] state) {
        super(sessionImplementor, entityName, verCfg, id);

        data = new HashMap<String, Object>();
        verCfg.getEntCfg().get(getEntityName()).getPropertyMapper().map(sessionImplementor, data,
				entityPersister.getPropertyNames(), state, null);
    }

    public AddWorkUnit(SessionImplementor sessionImplementor, String entityName, AuditConfiguration verCfg,
                       Serializable id, Map<String, Object> data) {
        super(sessionImplementor, entityName, verCfg, id);

        this.data = data;
    }

    public boolean containsWork() {
        return true;
    }

    public Map<String, Object> generateData(Object revisionData) {
        fillDataWithId(data, revisionData, RevisionType.ADD);
        return data;
    }

    public AuditWorkUnit merge(AddWorkUnit second) {
        return second;
    }

    public AuditWorkUnit merge(ModWorkUnit second) {
        return new AddWorkUnit(sessionImplementor, entityName, verCfg, id, second.getData());
    }

    public AuditWorkUnit merge(DelWorkUnit second) {
        return null;
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
