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

import org.jboss.envers.ModificationStore;
import org.jboss.envers.exception.VersionsException;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractCompositeIdMapper extends AbstractIdMapper implements SimpleIdMapperBuilder {
    protected Map<String, SingleIdMapper> ids;
    protected String compositeIdClass;

    protected AbstractCompositeIdMapper(String compositeIdClass) {
        ids = new LinkedHashMap<String, SingleIdMapper>();
        
        this.compositeIdClass = compositeIdClass;
    }

    public void add(String propertyName, ModificationStore modStore) {
        ids.put(propertyName, new SingleIdMapper(propertyName));
    }

    public Object mapToIdFromMap(Map data) {
        Object ret;
        try {
            ret = Thread.currentThread().getContextClassLoader().loadClass(compositeIdClass).newInstance();
        } catch (Exception e) {
            throw new VersionsException(e);
        }

        for (SingleIdMapper mapper : ids.values()) {
            mapper.mapToEntityFromMap(ret, data);
        }

        return ret;
    }
}
