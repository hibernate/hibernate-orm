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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.configuration.AuditEntitiesConfiguration;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;

import org.hibernate.Session;
import org.hibernate.engine.CollectionEntry;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.collection.PersistentCollection;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class PersistentCollectionChangeWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
    private final List<PersistentCollectionChangeData> collectionChanges;
    private final String referencingPropertyName;

    public PersistentCollectionChangeWorkUnit(SessionImplementor sessionImplementor, String entityName,
											  AuditConfiguration auditCfg, PersistentCollection collection,
											  CollectionEntry collectionEntry, Serializable snapshot, Serializable id,
                                              String referencingPropertyName) {
        super(sessionImplementor, entityName, auditCfg, new PersistentCollectionChangeWorkUnitId(id, collectionEntry.getRole()));

		this.referencingPropertyName = referencingPropertyName;

        collectionChanges = auditCfg.getEntCfg().get(getEntityName()).getPropertyMapper()
                .mapCollectionChanges(referencingPropertyName, collection, snapshot, id);
    }

    public PersistentCollectionChangeWorkUnit(SessionImplementor sessionImplementor, String entityName,
                                              AuditConfiguration verCfg, Serializable id,
                                              List<PersistentCollectionChangeData> collectionChanges,
                                              String referencingPropertyName) {
        super(sessionImplementor, entityName, verCfg, id);

        this.collectionChanges = collectionChanges;
        this.referencingPropertyName = referencingPropertyName;
    }

    public boolean containsWork() {
        return collectionChanges != null && collectionChanges.size() != 0;
    }

    public Map<String, Object> generateData(Object revisionData) {
        throw new UnsupportedOperationException("Cannot generate data for a collection change work unit!");
    }

    @SuppressWarnings({"unchecked"})
    public void perform(Session session, Object revisionData) {
        AuditEntitiesConfiguration entitiesCfg = verCfg.getAuditEntCfg();

        for (PersistentCollectionChangeData persistentCollectionChangeData : collectionChanges) {
            // Setting the revision number
            ((Map<String, Object>) persistentCollectionChangeData.getData().get(entitiesCfg.getOriginalIdPropName()))
                    .put(entitiesCfg.getRevisionFieldName(), revisionData);

            session.save(persistentCollectionChangeData.getEntityName(), persistentCollectionChangeData.getData());
        }
    }

    public String getReferencingPropertyName() {
        return referencingPropertyName;
    }

    public List<PersistentCollectionChangeData> getCollectionChanges() {
        return collectionChanges;
    }

    public AuditWorkUnit merge(AddWorkUnit second) {
        return null;
    }

    public AuditWorkUnit merge(ModWorkUnit second) {
        return null;
    }

    public AuditWorkUnit merge(DelWorkUnit second) {
        return null;
    }

    public AuditWorkUnit merge(CollectionChangeWorkUnit second) {
        return null;
    }

    public AuditWorkUnit merge(FakeBidirectionalRelationWorkUnit second) {
        return null;
    }

    public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {
        if (first instanceof PersistentCollectionChangeWorkUnit) {
            PersistentCollectionChangeWorkUnit original = (PersistentCollectionChangeWorkUnit) first;

            // Merging the collection changes in both work units.

            // First building a map from the ids of the collection-entry-entities from the "second" collection changes,
            // to the PCCD objects. That way, we will be later able to check if an "original" collection change
            // should be added, or if it is overshadowed by a new one.
            Map<Object, PersistentCollectionChangeData> newChangesIdMap = new HashMap<Object, PersistentCollectionChangeData>();
            for (PersistentCollectionChangeData persistentCollectionChangeData : getCollectionChanges()) {
                newChangesIdMap.put(
                        getOriginalId(persistentCollectionChangeData),
                        persistentCollectionChangeData);
            }

            // This will be the list with the resulting (merged) changes.
            List<PersistentCollectionChangeData> mergedChanges = new ArrayList<PersistentCollectionChangeData>();

            // Including only those original changes, which are not overshadowed by new ones.
            for (PersistentCollectionChangeData originalCollectionChangeData : original.getCollectionChanges()) {
                if (!newChangesIdMap.containsKey(getOriginalId(originalCollectionChangeData))) {
                    mergedChanges.add(originalCollectionChangeData);
                }
            }

            // Finally adding all of the new changes to the end of the list
            mergedChanges.addAll(getCollectionChanges());

            return new PersistentCollectionChangeWorkUnit(sessionImplementor, entityName, verCfg, id, mergedChanges, 
                    referencingPropertyName);
        } else {
            throw new RuntimeException("Trying to merge a " + first + " with a PersitentCollectionChangeWorkUnit. " +
                    "This is not really possible.");
        }
    }

    private Object getOriginalId(PersistentCollectionChangeData persistentCollectionChangeData) {
        return persistentCollectionChangeData.getData().get(verCfg.getAuditEntCfg().getOriginalIdPropName());
    }

    /**
     * A unique identifier for a collection work unit. Consists of an id of the owning entity and the name of
     * the entity plus the name of the field (the role). This is needed because such collections aren't entities
     * in the "normal" mapping, but they are entities for Envers.
     */
    private static class PersistentCollectionChangeWorkUnitId implements Serializable {
        private static final long serialVersionUID = -8007831518629167537L;
        
        private final Serializable ownerId;
        private final String role;

        public PersistentCollectionChangeWorkUnitId(Serializable ownerId, String role) {
            this.ownerId = ownerId;
            this.role = role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PersistentCollectionChangeWorkUnitId that = (PersistentCollectionChangeWorkUnitId) o;

            if (ownerId != null ? !ownerId.equals(that.ownerId) : that.ownerId != null) return false;
            //noinspection RedundantIfStatement
            if (role != null ? !role.equals(that.role) : that.role != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = ownerId != null ? ownerId.hashCode() : 0;
            result = 31 * result + (role != null ? role.hashCode() : 0);
            return result;
        }
    }
}
