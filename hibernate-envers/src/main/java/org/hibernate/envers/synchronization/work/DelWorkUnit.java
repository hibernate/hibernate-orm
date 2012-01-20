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

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.tools.Tools;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class DelWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
    private final Object[] state;
    private final EntityPersister entityPersister;
    private final String[] propertyNames;

    public DelWorkUnit(SessionImplementor sessionImplementor, String entityName, AuditConfiguration verCfg,
					   Serializable id, EntityPersister entityPersister, Object[] state) {
        super(sessionImplementor, entityName, verCfg, id, RevisionType.DEL);

        this.state = state;
        this.entityPersister = entityPersister;
        this.propertyNames = entityPersister.getPropertyNames();
    }

    public boolean containsWork() {
        return true;
    }

    public Map<String, Object> generateData(Object revisionData) {
        Map<String, Object> data = new HashMap<String, Object>();
        fillDataWithId(data, revisionData);

		if (verCfg.getGlobalCfg().isStoreDataAtDelete()) {
			verCfg.getEntCfg().get(getEntityName()).getPropertyMapper().map(sessionImplementor, data,
					propertyNames, state, state);
		} else {
			verCfg.getEntCfg().get(getEntityName()).getPropertyMapper().map(sessionImplementor, data,
					propertyNames, null, state);
		}

        return data;
    }

    public AuditWorkUnit merge(AddWorkUnit second) {
        if (Tools.arraysEqual(second.getState(), state)) {
            return null; // Return null if object's state has not changed.
        }
        return new ModWorkUnit(sessionImplementor, entityName, verCfg, id, entityPersister, second.getState(), state); 
    }

    public AuditWorkUnit merge(ModWorkUnit second) {
        return null;
    }

    public AuditWorkUnit merge(DelWorkUnit second) {
        return this;
    }

    public AuditWorkUnit merge(CollectionChangeWorkUnit second) {
        return this;
    }

    public AuditWorkUnit merge(FakeBidirectionalRelationWorkUnit second) {
        return this;
    }

    public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {
        return first.merge(this);
    }
}