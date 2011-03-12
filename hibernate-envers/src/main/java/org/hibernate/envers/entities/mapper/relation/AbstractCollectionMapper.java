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
package org.hibernate.envers.entities.mapper.relation;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.entities.mapper.PropertyMapper;
import org.hibernate.envers.entities.mapper.relation.lazy.initializor.Initializor;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.reflection.ReflectionTools;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.property.Setter;
import org.hibernate.engine.SessionImplementor;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractCollectionMapper<T> implements PropertyMapper {
    protected final CommonCollectionMapperData commonCollectionMapperData;    
    protected final Class<? extends T> collectionClass;

    private final Constructor<? extends T> proxyConstructor;

    protected AbstractCollectionMapper(CommonCollectionMapperData commonCollectionMapperData,
                                       Class<? extends T> collectionClass, Class<? extends T> proxyClass) {
        this.commonCollectionMapperData = commonCollectionMapperData;
        this.collectionClass = collectionClass;

        try {
            proxyConstructor = proxyClass.getConstructor(Initializor.class);
        } catch (NoSuchMethodException e) {
            throw new AuditException(e);
        }
    }

    protected abstract Collection getNewCollectionContent(PersistentCollection newCollection);
    protected abstract Collection getOldCollectionContent(Serializable oldCollection);

    /**
     * Maps the changed collection element to the given map.
     * @param data Where to map the data.
     * @param changed The changed collection element to map.
     */
    protected abstract void mapToMapFromObject(Map<String, Object> data, Object changed);

    private void addCollectionChanges(List<PersistentCollectionChangeData> collectionChanges, Set<Object> changed,
                                      RevisionType revisionType, Serializable id) {
        for (Object changedObj : changed) {
            Map<String, Object> entityData = new HashMap<String, Object>();
            Map<String, Object> originalId = new HashMap<String, Object>();
            entityData.put(commonCollectionMapperData.getVerEntCfg().getOriginalIdPropName(), originalId);

            collectionChanges.add(new PersistentCollectionChangeData(
                    commonCollectionMapperData.getVersionsMiddleEntityName(), entityData, changedObj));
            // Mapping the collection owner's id.
            commonCollectionMapperData.getReferencingIdData().getPrefixedMapper().mapToMapFromId(originalId, id);

            // Mapping collection element and index (if present).
            mapToMapFromObject(originalId, changedObj);

            entityData.put(commonCollectionMapperData.getVerEntCfg().getRevisionTypePropName(), revisionType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                     PersistentCollection newColl,
                                                                     Serializable oldColl, Serializable id) {
        if (!commonCollectionMapperData.getCollectionReferencingPropertyData().getName()
                .equals(referencingPropertyName)) {
            return null;
        }

        List<PersistentCollectionChangeData> collectionChanges = new ArrayList<PersistentCollectionChangeData>();

        // Comparing new and old collection content.
        Collection newCollection = getNewCollectionContent(newColl);
        Collection oldCollection = getOldCollectionContent(oldColl);

        Set<Object> added = new HashSet<Object>();
        if (newColl != null) { added.addAll(newCollection); }
		// Re-hashing the old collection as the hash codes of the elements there may have changed, and the
		// removeAll in AbstractSet has an implementation that is hashcode-change sensitive (as opposed to addAll).
        if (oldColl != null) { added.removeAll(new HashSet(oldCollection)); }

        addCollectionChanges(collectionChanges, added, RevisionType.ADD, id);

        Set<Object> deleted = new HashSet<Object>();
        if (oldColl != null) { deleted.addAll(oldCollection); }
		// The same as above - re-hashing new collection.
        if (newColl != null) { deleted.removeAll(new HashSet(newCollection)); }

        addCollectionChanges(collectionChanges, deleted, RevisionType.DEL, id);

        return collectionChanges;
    }

    public boolean mapToMapFromEntity(SessionImplementor session, Map<String, Object> data, Object newObj, Object oldObj) {
        // Changes are mapped in the "mapCollectionChanges" method.
        return false;
    }

    protected abstract Initializor<T> getInitializor(AuditConfiguration verCfg,
                                                     AuditReaderImplementor versionsReader, Object primaryKey,
                                                     Number revision);

    public void mapToEntityFromMap(AuditConfiguration verCfg, Object obj, Map data, Object primaryKey,
                                   AuditReaderImplementor versionsReader, Number revision) {
        Setter setter = ReflectionTools.getSetter(obj.getClass(),
                commonCollectionMapperData.getCollectionReferencingPropertyData());
        try {
            setter.set(obj, proxyConstructor.newInstance(getInitializor(verCfg, versionsReader, primaryKey, revision)), null);
        } catch (InstantiationException e) {
            throw new AuditException(e);
        } catch (IllegalAccessException e) {
            throw new AuditException(e);
        } catch (InvocationTargetException e) {
            throw new AuditException(e);
        }
    }
}
