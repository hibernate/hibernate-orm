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
import java.util.List;
import java.util.Map;

import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.reflection.ReflectionTools;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.property.Setter;
import org.hibernate.util.ReflectHelper;
import org.hibernate.engine.SessionImplementor;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ComponentPropertyMapper implements PropertyMapper, CompositeMapperBuilder {
    private final PropertyData propertyData;
    private final MultiPropertyMapper delegate;
	private final String componentClassName;

    public ComponentPropertyMapper(PropertyData propertyData, String componentClassName) {
        this.propertyData = propertyData;
        this.delegate = new MultiPropertyMapper();
		this.componentClassName = componentClassName;
    }

	public void add(PropertyData propertyData) {
        delegate.add(propertyData);
    }

    public CompositeMapperBuilder addComponent(PropertyData propertyData, String componentClassName) {
        return delegate.addComponent(propertyData, componentClassName);
    }

    public void addComposite(PropertyData propertyData, PropertyMapper propertyMapper) {
        delegate.addComposite(propertyData, propertyMapper);
    }

    public boolean mapToMapFromEntity(SessionImplementor session, Map<String, Object> data, Object newObj, Object oldObj) {
        return delegate.mapToMapFromEntity(session, data, newObj, oldObj);
    }

    public void mapToEntityFromMap(AuditConfiguration verCfg, Object obj, Map data, Object primaryKey, AuditReaderImplementor versionsReader, Number revision) {
        if (data == null || obj == null) {
            return;
        }

        Setter setter = ReflectionTools.getSetter(obj.getClass(), propertyData);

		// If all properties are null and single, then the component has to be null also.
		boolean allNullAndSingle = true;
		for (Map.Entry<PropertyData, PropertyMapper> property : delegate.getProperties().entrySet()) {
			if (data.get(property.getKey().getName()) != null || !(property.getValue() instanceof SinglePropertyMapper)) {
				allNullAndSingle = false;
				break;
			}
		}

		// And we don't have to set anything on the object - the default value is null
		if (!allNullAndSingle) {
			try {
				Object subObj = ReflectHelper.getDefaultConstructor(
						Thread.currentThread().getContextClassLoader().loadClass(componentClassName)).newInstance();
				setter.set(obj, subObj, null);
				delegate.mapToEntityFromMap(verCfg, subObj, data, primaryKey, versionsReader, revision);
			} catch (Exception e) {
				throw new AuditException(e);
			}
		}
    }

    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                                    PersistentCollection newColl,
                                                                                    Serializable oldColl,
                                                                                    Serializable id) {
        return delegate.mapCollectionChanges(referencingPropertyName, newColl, oldColl, id);
    }

}
