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
package org.hibernate.envers.entities.mapper.id;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.tools.reflection.ReflectionTools;

import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.util.ReflectHelper;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EmbeddedIdMapper extends AbstractCompositeIdMapper implements SimpleIdMapperBuilder {
    private String idPropertyName;

    public EmbeddedIdMapper(String idPropertyName, String compositeIdClass) {
        super(compositeIdClass);
        
        this.idPropertyName = idPropertyName;
    }

    public void mapToMapFromId(Map<String, Object> data, Object obj) {
        for (IdMapper idMapper : ids.values()) {
            idMapper.mapToMapFromEntity(data, obj);
        }
    }

    public void mapToMapFromEntity(Map<String, Object> data, Object obj) {
        if (obj == null) {
            return;
        }

        Getter getter = ReflectionTools.getGetter(obj.getClass(), idPropertyName);
        mapToMapFromId(data, getter.get(obj));
    }

    public void mapToEntityFromMap(Object obj, Map data) {
        if (data == null || obj == null) {
            return;
        }

        Getter getter = ReflectionTools.getGetter(obj.getClass(), idPropertyName);
        Setter setter = ReflectionTools.getSetter(obj.getClass(), idPropertyName);

        try {
            Object subObj = ReflectHelper.getDefaultConstructor(getter.getReturnType()).newInstance();
            setter.set(obj, subObj, null);

            for (IdMapper idMapper : ids.values()) {
                idMapper.mapToEntityFromMap(subObj, data);
            }
        } catch (Exception e) {
            throw new AuditException(e);
        }
    }

    public IdMapper prefixMappedProperties(String prefix) {
        EmbeddedIdMapper ret = new EmbeddedIdMapper(idPropertyName, compositeIdClass);

        for (String propertyName : ids.keySet()) {
            ret.ids.put(propertyName, new SingleIdMapper(propertyName, prefix + propertyName));
        }

        return ret;
    }

    public Object mapToIdFromEntity(Object data) {
        if (data == null) {
            return null;
        }

        Getter getter = ReflectionTools.getGetter(data.getClass(), idPropertyName);
        return getter.get(data);
    }

    public List<QueryParameterData> mapToQueryParametersFromId(Object obj) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        mapToMapFromId(data, obj);

        List<QueryParameterData> ret = new ArrayList<QueryParameterData>();

        for (Map.Entry<String, Object> propertyData : data.entrySet()) {
            ret.add(new QueryParameterData(propertyData.getKey(), propertyData.getValue()));
        }

        return ret;
    }
}
