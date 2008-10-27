/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 *
 * See the copyright.txt in the distribution for a  full listing of individual
 * contributors. This copyrighted material is made available to anyone wishing
 * to use,  modify, copy, or redistribute it subject to the terms and
 * conditions of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT A WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.relation;

import org.jboss.envers.entities.mapper.PersistentCollectionChangeData;
import org.jboss.envers.entities.mapper.PropertyMapper;
import org.jboss.envers.entities.mapper.relation.lazy.initializor.Initializor;
import org.jboss.envers.RevisionType;
import org.jboss.envers.exception.VersionsException;
import org.jboss.envers.tools.reflection.ReflectionTools;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.configuration.VersionsConfiguration;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.property.Setter;

import java.util.*;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;

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
            throw new VersionsException(e);
        }
    }

    protected abstract Collection getNewCollectionContent(PersistentCollection newCollection);
    protected abstract Collection getOldCollectionContent(Serializable oldCollection);
    protected abstract Object getElement(Object changedObject);

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
                    commonCollectionMapperData.getVersionsMiddleEntityName(), entityData, getElement(changedObj)));
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
        if (!commonCollectionMapperData.getCollectionReferencingPropertyName().equals(referencingPropertyName)) {
            return null;
        }

        List<PersistentCollectionChangeData> collectionChanges = new ArrayList<PersistentCollectionChangeData>();

        // Comparing new and old collection content.
        Collection newCollection = getNewCollectionContent(newColl);
        Collection oldCollection = getOldCollectionContent(oldColl);

        Set<Object> added = new HashSet<Object>();
        if (newColl != null) { added.addAll(newCollection); }
        if (oldColl != null) { added.removeAll(oldCollection); }

        addCollectionChanges(collectionChanges, added, RevisionType.ADD, id);

        Set<Object> deleted = new HashSet<Object>();
        if (oldColl != null) { deleted.addAll(oldCollection); }
        if (newColl != null) { deleted.removeAll(newCollection); }

        addCollectionChanges(collectionChanges, deleted, RevisionType.DEL, id);

        return collectionChanges;
    }

    public boolean mapToMapFromEntity(Map<String, Object> data, Object newObj, Object oldObj) {
        // Changes are mapped in the "mapCollectionChanges" method.
        return false;
    }

    protected abstract Initializor<T> getInitializor(VersionsConfiguration verCfg,
                                                     VersionsReaderImplementor versionsReader, Object primaryKey,
                                                     Number revision);

    public void mapToEntityFromMap(VersionsConfiguration verCfg, Object obj, Map data, Object primaryKey,
                                   VersionsReaderImplementor versionsReader, Number revision) {
        Setter setter = ReflectionTools.getSetter(obj.getClass(),
                commonCollectionMapperData.getCollectionReferencingPropertyName());
        try {
            setter.set(obj, proxyConstructor.newInstance(getInitializor(verCfg, versionsReader, primaryKey, revision)), null);
        } catch (InstantiationException e) {
            throw new VersionsException(e);
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }
    }
}
