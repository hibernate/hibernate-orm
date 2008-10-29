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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.VersionsConfiguration;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.entities.mapper.PropertyMapper;
import org.hibernate.envers.entities.mapper.id.IdMapper;
import org.hibernate.envers.entities.mapper.relation.lazy.ToOneDelegateSessionImplementor;
import org.hibernate.envers.reader.VersionsReaderImplementor;
import org.hibernate.envers.tools.Tools;
import org.hibernate.envers.tools.reflection.ReflectionTools;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.property.Setter;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ToOneIdMapper implements PropertyMapper {
    private final IdMapper delegate;
    private final String propertyName;
    private final String referencedEntityName;

    public ToOneIdMapper(IdMapper delegate, String propertyName, String referencedEntityName) {
        this.delegate = delegate;
        this.propertyName = propertyName;
        this.referencedEntityName = referencedEntityName;
    }

    public boolean mapToMapFromEntity(Map<String, Object> data, Object newObj, Object oldObj) {
        HashMap<String, Object> newData = new HashMap<String, Object>();
        data.put(propertyName, newData);

        delegate.mapToMapFromEntity(newData, newObj);

        return !Tools.objectsEqual(newObj, oldObj);
    }

    public void mapToEntityFromMap(VersionsConfiguration verCfg, Object obj, Map data, Object primaryKey,
                                   VersionsReaderImplementor versionsReader, Number revision) {
        if (obj == null) {
            return;
        }

        Object entityId = delegate.mapToIdFromMap((Map) data.get(propertyName));
        Object value;
        if (entityId == null) {
            value = null;
        } else {
            if (versionsReader.getFirstLevelCache().contains(referencedEntityName, revision, entityId)) {
                value = versionsReader.getFirstLevelCache().get(referencedEntityName, revision, entityId);
            } else {
                Class<?> entityClass = ReflectionTools.loadClass(referencedEntityName);

                value = versionsReader.getSessionImplementor().getFactory().getEntityPersister(referencedEntityName).
                        createProxy(null, new ToOneDelegateSessionImplementor(versionsReader, entityClass, entityId, revision));
            }
        }

        Setter setter = ReflectionTools.getSetter(obj.getClass(), propertyName);
        setter.set(obj, value, null);
    }

    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                     PersistentCollection newColl,
                                                                     Serializable oldColl,
                                                                     Serializable id) {
        return null;
    }
}
