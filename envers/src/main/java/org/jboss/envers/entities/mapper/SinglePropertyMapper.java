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
package org.jboss.envers.entities.mapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.jboss.envers.ModificationStore;
import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.exception.VersionsException;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.tools.Tools;
import org.jboss.envers.tools.reflection.ReflectionTools;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.property.Setter;

/**
 * TODO: diff
 * @author Adam Warski (adam at warski dot org)
 */
public class SinglePropertyMapper implements PropertyMapper, SimpleMapperBuilder {
    private String propertyName;

    public SinglePropertyMapper() { }

    public void add(String propertyName, ModificationStore modStore) {
        if (this.propertyName != null) {
            throw new VersionsException("Only one property can be added!");
        }

        this.propertyName = propertyName;
    }

    public boolean mapToMapFromEntity(Map<String, Object> data, Object newObj, Object oldObj) {
        data.put(propertyName, newObj);

        return !Tools.objectsEqual(newObj, oldObj);
    }

    public void mapToEntityFromMap(VersionsConfiguration verCfg, Object obj, Map data, Object primaryKey, VersionsReaderImplementor versionsReader, Number revision) {
        if (data == null || obj == null) {
            return;
        }

        Setter setter = ReflectionTools.getSetter(obj.getClass(), propertyName);
        setter.set(obj, data.get(propertyName), null);
    }

    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                                    PersistentCollection newColl,
                                                                                    Serializable oldColl,
                                                                                    Serializable id) {
        return null;
    }

}
