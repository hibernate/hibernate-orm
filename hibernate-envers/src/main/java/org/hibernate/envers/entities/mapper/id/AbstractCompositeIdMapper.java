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

import java.util.Map;

import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.tools.Tools;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractCompositeIdMapper extends AbstractIdMapper implements SimpleIdMapperBuilder {
    protected Map<PropertyData, SingleIdMapper> ids;
    protected String compositeIdClass;

    protected AbstractCompositeIdMapper(String compositeIdClass) {
        ids = Tools.newLinkedHashMap();
        
        this.compositeIdClass = compositeIdClass;
    }

    public void add(PropertyData propertyData) {
        ids.put(propertyData, new SingleIdMapper(propertyData));
    }

    public Object mapToIdFromMap(Map data) {
        Object ret;
        try {
            ret = Thread.currentThread().getContextClassLoader().loadClass(compositeIdClass).newInstance();
        } catch (Exception e) {
            throw new AuditException(e);
        }

        for (SingleIdMapper mapper : ids.values()) {
            mapper.mapToEntityFromMap(ret, data);
        }

        return ret;
    }
}
