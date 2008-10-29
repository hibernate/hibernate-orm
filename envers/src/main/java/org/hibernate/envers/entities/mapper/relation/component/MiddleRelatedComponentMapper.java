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

import org.hibernate.envers.entities.EntityInstantiator;
import org.hibernate.envers.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.tools.query.Parameters;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public final class MiddleRelatedComponentMapper implements MiddleComponentMapper {
    private final MiddleIdData relatedIdData;

    public MiddleRelatedComponentMapper(MiddleIdData relatedIdData) {
        this.relatedIdData = relatedIdData;
    }

    public Object mapToObjectFromFullMap(EntityInstantiator entityInstantiator, Map<String, Object> data,
                                         Object dataObject, Number revision) {
        return entityInstantiator.createInstanceFromVersionsEntity(relatedIdData.getEntityName(), data, revision);
    }

    public void mapToMapFromObject(Map<String, Object> data, Object obj) {
        relatedIdData.getPrefixedMapper().mapToMapFromEntity(data, obj);
    }

    public void addMiddleEqualToQuery(Parameters parameters, String prefix1, String prefix2) {
        relatedIdData.getPrefixedMapper().addIdsEqualToQuery(parameters, prefix1, prefix2);
    }
}
