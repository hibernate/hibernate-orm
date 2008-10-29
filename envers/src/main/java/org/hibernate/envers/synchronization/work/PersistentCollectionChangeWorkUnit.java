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
package org.jboss.envers.synchronization.work;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.configuration.VersionsEntitiesConfiguration;
import org.jboss.envers.entities.mapper.PersistentCollectionChangeData;

import org.hibernate.Session;
import org.hibernate.collection.PersistentCollection;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class PersistentCollectionChangeWorkUnit extends AbstractVersionsWorkUnit implements VersionsWorkUnit {
    private final List<PersistentCollectionChangeData> collectionChanges;
    private final String referencingPropertyName;

    public PersistentCollectionChangeWorkUnit(String entityName, VersionsConfiguration verCfg,
                                              PersistentCollection collection, String role,
                                              Serializable snapshot, Serializable id) {
        super(entityName, verCfg, null);

        referencingPropertyName = role.substring(entityName.length() + 1);

        collectionChanges = verCfg.getEntCfg().get(getEntityName()).getPropertyMapper()
                .mapCollectionChanges(referencingPropertyName, collection, snapshot, id);
    }

    public boolean containsWork() {
        return collectionChanges != null && collectionChanges.size() != 0;
    }

    @SuppressWarnings({"unchecked"})
    public void perform(Session session, Object revisionData) {
        VersionsEntitiesConfiguration entitiesCfg = verCfg.getVerEntCfg();

        for (PersistentCollectionChangeData persistentCollectionChangeData : collectionChanges) {
            // Setting the revision number
            ((Map<String, Object>) persistentCollectionChangeData.getData().get(entitiesCfg.getOriginalIdPropName()))
                    .put(entitiesCfg.getRevisionPropName(), revisionData);

            session.save(persistentCollectionChangeData.getEntityName(), persistentCollectionChangeData.getData());
        }
    }

    public String getReferencingPropertyName() {
        return referencingPropertyName;
    }

    public List<PersistentCollectionChangeData> getCollectionChanges() {
        return collectionChanges;
    }

    public KeepCheckResult check(AddWorkUnit second) {
        return null;
    }

    public KeepCheckResult check(ModWorkUnit second) {
        return null;
    }

    public KeepCheckResult check(DelWorkUnit second) {
        return null;
    }

    public KeepCheckResult check(CollectionChangeWorkUnit second) {
        return null;
    }

    public KeepCheckResult dispatch(KeepCheckVisitor first) {
        return null;
    }
}
