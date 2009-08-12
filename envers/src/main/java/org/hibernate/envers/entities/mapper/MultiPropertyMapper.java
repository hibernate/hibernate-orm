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
import java.util.Set;

import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.reflection.ReflectionTools;
import org.hibernate.envers.tools.Tools;
import org.hibernate.envers.tools.MappingTools;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.property.Getter;
import org.hibernate.engine.SessionImplementor;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MultiPropertyMapper implements ExtendedPropertyMapper {
    protected final Map<PropertyData, PropertyMapper> properties;
    private final Map<String, PropertyData> propertyDatas;

    public MultiPropertyMapper() {
        properties = Tools.newHashMap();
        propertyDatas = Tools.newHashMap();
    }

    public void add(PropertyData propertyData) {
        SinglePropertyMapper single = new SinglePropertyMapper();
        single.add(propertyData);
        properties.put(propertyData, single);
        propertyDatas.put(propertyData.getName(), propertyData);
    }

    public CompositeMapperBuilder addComponent(PropertyData propertyData, String componentClassName) {
        if (properties.get(propertyData) != null) {
			// This is needed for second pass to work properly in the components mapper
            return (CompositeMapperBuilder) properties.get(propertyData);
        }

        ComponentPropertyMapper componentMapperBuilder = new ComponentPropertyMapper(propertyData, componentClassName);
		addComposite(propertyData, componentMapperBuilder);

        return componentMapperBuilder;
    }

    public void addComposite(PropertyData propertyData, PropertyMapper propertyMapper) {
        properties.put(propertyData, propertyMapper);
        propertyDatas.put(propertyData.getName(), propertyData);
    }

    private Object getAtIndexOrNull(Object[] array, int index) { return array == null ? null : array[index]; }

    public boolean map(SessionImplementor session, Map<String, Object> data, String[] propertyNames, Object[] newState, Object[] oldState) {
        boolean ret = false;
        for (int i=0; i<propertyNames.length; i++) {
            String propertyName = propertyNames[i];

            if (propertyDatas.containsKey(propertyName)) {
                ret |= properties.get(propertyDatas.get(propertyName)).mapToMapFromEntity(session, data,
                        getAtIndexOrNull(newState, i),
                        getAtIndexOrNull(oldState, i));
            }
        }

        return ret;
    }

    public boolean mapToMapFromEntity(SessionImplementor session, Map<String, Object> data, Object newObj, Object oldObj) {
        boolean ret = false;
        for (PropertyData propertyData : properties.keySet()) {
            Getter getter;
            if (newObj != null) {
                getter = ReflectionTools.getGetter(newObj.getClass(), propertyData);
            } else if (oldObj != null) {
                getter = ReflectionTools.getGetter(oldObj.getClass(), propertyData);
            } else {
                return false;
            }

            ret |= properties.get(propertyData).mapToMapFromEntity(session, data,
                    newObj == null ? null : getter.get(newObj),
                    oldObj == null ? null : getter.get(oldObj));
        }

        return ret;
    }

    public void mapToEntityFromMap(AuditConfiguration verCfg, Object obj, Map data, Object primaryKey,
                                   AuditReaderImplementor versionsReader, Number revision) {
        for (PropertyMapper mapper : properties.values()) {
            mapper.mapToEntityFromMap(verCfg, obj, data, primaryKey, versionsReader, revision);
        }
    }

    public List<PersistentCollectionChangeData> mapCollectionChanges(String referencingPropertyName,
                                                                                    PersistentCollection newColl,
                                                                                    Serializable oldColl,
                                                                                    Serializable id) {
		// Name of the properyt, to which we will delegate the mapping.
		String delegatePropertyName;

		// Checking if the property name doesn't reference a collection in a component - then the name will containa a .
		int dotIndex = referencingPropertyName.indexOf('.');
		if (dotIndex != -1) {
			// Computing the name of the component
			String componentName = referencingPropertyName.substring(0, dotIndex);
			// And the name of the property in the component
			String propertyInComponentName = MappingTools.createComponentPrefix(componentName)
					+ referencingPropertyName.substring(dotIndex+1);

			// We need to get the mapper for the component.
			referencingPropertyName = componentName;
			// As this is a component, we delegate to the property in the component.
			delegatePropertyName = propertyInComponentName;
		} else {
			// If this is not a component, we delegate to the same property.
			delegatePropertyName = referencingPropertyName;
		}

        PropertyMapper mapper = properties.get(propertyDatas.get(referencingPropertyName));
        if (mapper != null) {
            return mapper.mapCollectionChanges(delegatePropertyName, newColl, oldColl, id);
        } else {
            return null;
        }
    }

	public Map<PropertyData, PropertyMapper> getProperties() {
		return properties;
	}
}
