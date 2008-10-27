/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.synchronization.work;

import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.RevisionType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.Session;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ModWorkUnit extends AbstractVersionsWorkUnit implements VersionsWorkUnit {
    private final Map<String, Object> data;
    private final boolean changes;        

    public ModWorkUnit(String entityName, VersionsConfiguration verCfg, Serializable id,
                       EntityPersister entityPersister, Object[] newState, Object[] oldState) {
        super(entityName, verCfg, id);

        data = new HashMap<String, Object>();
        changes = verCfg.getEntCfg().get(getEntityName()).getPropertyMapper().map(data, entityPersister.getPropertyNames(),
                newState, oldState);
    }

    public boolean containsWork() {
        return changes;
    }

    public void perform(Session session, Object revisionData) {
        fillDataWithId(data, revisionData, RevisionType.MOD);

        session.save(verCfg.getVerEntCfg().getVersionsEntityName(getEntityName()), data);

        setPerformed(data);
    }

    public KeepCheckResult check(AddWorkUnit second) {
        return KeepCheckResult.FIRST;
    }

    public KeepCheckResult check(ModWorkUnit second) {
        return KeepCheckResult.SECOND;
    }

    public KeepCheckResult check(DelWorkUnit second) {
        return KeepCheckResult.SECOND;
    }

    public KeepCheckResult check(CollectionChangeWorkUnit second) {
        return KeepCheckResult.FIRST;
    }

    public KeepCheckResult dispatch(KeepCheckVisitor first) {
        return first.check(this);
    }
}