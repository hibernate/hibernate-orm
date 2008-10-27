/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.id;

import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.util.ReflectHelper;
import org.jboss.envers.exception.VersionsException;
import org.jboss.envers.tools.reflection.ReflectionTools;

import java.util.*;

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
            throw new VersionsException(e);
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
