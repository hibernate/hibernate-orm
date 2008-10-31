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
package org.hibernate.envers.entities.mapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.reflection.ReflectionTools;

import org.hibernate.MappingException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.property.Getter;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MultiPropertyMapper implements ExtendedPropertyMapper {
    protected Map<String, PropertyMapper> properties;

    public MultiPropertyMapper() {
        properties = new HashMap<String, PropertyMapper>();
    }

    public void add(String propertyName, ModificationStore modStore) {
        SinglePropertyMapper single = new SinglePropertyMapper();
        single.add(propertyName,  modStore);
        properties.put(propertyName, single);
    }

    public CompositeMapperBuilder addComposite(String propertyName) {
        if (properties.get(propertyName) != null) {
            throw new MappingException("Mapping for " + propertyName + " already added!");
        }

        MapPropertyMapper mapperBuilder = new MapPropertyMapper(propertyName);
        properties.put(propertyName, mapperBuilder);

        return mapperBuilder;
    }

    public void addComposite(String propertyName, PropertyMapper propertyMapper) {
        properties.put(propertyName, propertyMapper);
    }

    private Object getAtIndexOrNull(Object[] array, int index) { return array == null ? null : array[index]; }

    public boolean map(Map<String, Object> data, String[] propertyNames, Object[] newState, Object[] oldState) {
        boolean ret = false;
        for (int i=0; i<propertyNames.length; i++) {
            String propertyName = propertyNames[i];

            if (properties.containsKey(propertyName)) {
                ret |= properties.get(propertyName).mapToMapFromEntity(data,
                        getAtIndexOrNull(newState, i),
                        getAtIndexOrNull(oldState, i));
            }
        }

        return ret;
    }

    public boolean mapToMapFromEntity(Map<String, Object> data, Object newObj, Object oldObj) {
        boolean ret = false;
        for (String propertyName : properties.keySet()) {
            Getter getter;
            if (newObj != null) {
                getter = ReflectionTools.getGetter(newObj.getClass(), propertyName);
            } else if (oldObj != null) {
                getter = ReflectionTools.getGetter(oldObj.getClass(), propertyName);
            } else {
                return false;
            }

            ret |= properties.get(propertyName).mapToMapFromEntity(data,
                    newObj == null ? null : getter.get(newObj),
                    oldObj == null ? null : getter.get(oldObj));
        }

        return ret;
    }

    public void mapToEntityFromMap(AuditConfiguration verCfg, Object obj, Map data, Object primaryKey, AuditReaderImplementor versionsReader, Number revision) {
        for (String propertyName : properties.keySet()) {
            properties.get(propertyName).mapToEntityFromMap(verCfg, obj, data, primaryKey, versionsReader, revision);
        }
    }

    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                                    PersistentCollection newColl,
                                                                                    Serializable oldColl,
                                                                                    Serializable id) {
        PropertyMapper mapper = properties.get(referencingPropertyName);
        if (mapper != null) {
            return mapper.mapCollectionChanges(referencingPropertyName, newColl, oldColl, id);
        } else {
            return null;
        }
    }
}
