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
package org.hibernate.envers.entities.mapper.relation;

import org.hibernate.envers.configuration.AuditEntitiesConfiguration;
import org.hibernate.envers.entities.IdMappingData;
import org.hibernate.envers.entities.mapper.id.IdMapper;

/**
 * A class holding information about ids, which form a virtual "relation" from a middle-table. Middle-tables are used
 * when mapping collections.
 * @author Adam Warski (adam at warski dot org)
 */
public final class MiddleIdData {
    /**
     * Original id mapper of the related entity.
     */
    private final IdMapper originalMapper;
    /**
     * Prefixed id mapper (with the names for the id fields that are used in the middle table) of the related entity.
     */
    private final IdMapper prefixedMapper;
    /**
     * Name of the related entity.
     */
    private final String entityName;
    /**
     * Versions name of the related entity.
     */
    private final String versionsEntityName;

    public MiddleIdData(AuditEntitiesConfiguration verEntCfg, IdMappingData mappingData, String prefix,
                        String entityName) {
        this.originalMapper = mappingData.getIdMapper();
        this.prefixedMapper = mappingData.getIdMapper().prefixMappedProperties(prefix);
        this.entityName = entityName;
        this.versionsEntityName = verEntCfg.getVersionsEntityName(entityName);
    }

    public IdMapper getOriginalMapper() {
        return originalMapper;
    }

    public IdMapper getPrefixedMapper() {
        return prefixedMapper;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getVersionsEntityName() {
        return versionsEntityName;
    }
}
