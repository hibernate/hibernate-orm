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
package org.hibernate.envers.entities.mapper.relation.component;

import java.util.Map;

import org.hibernate.envers.configuration.AuditEntitiesConfiguration;
import org.hibernate.envers.entities.EntityInstantiator;
import org.hibernate.envers.tools.query.Parameters;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public final class MiddleSimpleComponentMapper implements MiddleComponentMapper {
    private final String propertyName;
    private final AuditEntitiesConfiguration verEntCfg;

    public MiddleSimpleComponentMapper(AuditEntitiesConfiguration verEntCfg, String propertyName) {
        this.propertyName = propertyName;
        this.verEntCfg = verEntCfg;
    }

    @SuppressWarnings({"unchecked"})
    public Object mapToObjectFromFullMap(EntityInstantiator entityInstantiator, Map<String, Object> data,
                                         Object dataObject, Number revision) {
        return ((Map<String, Object>) data.get(verEntCfg.getOriginalIdPropName())).get(propertyName);
    }

    public void mapToMapFromObject(Map<String, Object> data, Object obj) {
        data.put(propertyName, obj);
    }

    public void addMiddleEqualToQuery(Parameters parameters, String prefix1, String prefix2) {
        parameters.addWhere(prefix1 + "." + propertyName, false, "=", prefix2 + "." + propertyName, false);
    }
}
