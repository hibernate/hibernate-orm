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

import org.hibernate.Session;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AddWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
    private final Object[] state;
    private final String[] propertyNames;

    public AddWorkUnit(String entityName, AuditConfiguration verCfg, Serializable id,
                       EntityPersister entityPersister, Object[] state) {
        super(entityName, verCfg, id);

        this.state = state;
        this.propertyNames = entityPersister.getPropertyNames();
    }

    public boolean containsWork() {
        return true;
    }

    public void perform(Session session, Object revisionData) {
        Map<String, Object> data = new HashMap<String, Object>();
        fillDataWithId(data, revisionData, RevisionType.ADD);

        verCfg.getEntCfg().get(getEntityName()).getPropertyMapper().map(data, propertyNames, state, null);

        session.save(verCfg.getAuditEntCfg().getVersionsEntityName(getEntityName()), data);

        setPerformed(data);
    }

    public KeepCheckResult check(AddWorkUnit second) {
        return KeepCheckResult.FIRST;
    }

    public KeepCheckResult check(ModWorkUnit second) {
        return KeepCheckResult.SECOND;
    }

    public KeepCheckResult check(DelWorkUnit second) {
        return KeepCheckResult.NONE;
    }

    public KeepCheckResult check(CollectionChangeWorkUnit second) {
        return KeepCheckResult.FIRST;
    }

    public KeepCheckResult dispatch(KeepCheckVisitor first) {
        return first.check(this);
    }
}
