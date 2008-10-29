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
import org.hibernate.envers.configuration.VersionsConfiguration;
import org.hibernate.envers.exception.VersionsException;
import org.hibernate.envers.reader.VersionsReaderImplementor;
import org.hibernate.envers.tools.reflection.ReflectionTools;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.util.ReflectHelper;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MapPropertyMapper implements PropertyMapper, CompositeMapperBuilder {
    private String propertyName;
    private ExtendedPropertyMapper delegate;

    public MapPropertyMapper(String propertyName) {
        this.propertyName = propertyName;
        this.delegate = new MultiPropertyMapper();
    }

    public void add(String propertyName, ModificationStore modStore) {
        delegate.add(propertyName, modStore);
    }

    public CompositeMapperBuilder addComposite(String propertyName) {
        return delegate.addComposite(propertyName);
    }

    public void addComposite(String propertyName, PropertyMapper propertyMapper) {
        delegate.addComposite(propertyName, propertyMapper);
    }

    public boolean mapToMapFromEntity(Map<String, Object> data, Object newObj, Object oldObj) {
        Map<String, Object> newData = new HashMap<String, Object>();
        data.put(propertyName, newData);

        return delegate.mapToMapFromEntity(newData, newObj, oldObj);
    }

    public void mapToEntityFromMap(VersionsConfiguration verCfg, Object obj, Map data, Object primaryKey, VersionsReaderImplementor versionsReader, Number revision) {
        if (data == null || obj == null) {
            return;
        }

        Getter getter = ReflectionTools.getGetter(obj.getClass(), propertyName);
        Setter setter = ReflectionTools.getSetter(obj.getClass(), propertyName);

        try {
            Object subObj = ReflectHelper.getDefaultConstructor(getter.getReturnType()).newInstance();
            setter.set(obj, subObj, null);
            delegate.mapToEntityFromMap(verCfg, subObj, (Map) data.get(propertyName), primaryKey, versionsReader, revision);
        } catch (Exception e) {
            throw new VersionsException(e);
        }
    }

    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                                    PersistentCollection newColl,
                                                                                    Serializable oldColl,
                                                                                    Serializable id) {
        return delegate.mapCollectionChanges(referencingPropertyName, newColl, oldColl, id);
    }

}
