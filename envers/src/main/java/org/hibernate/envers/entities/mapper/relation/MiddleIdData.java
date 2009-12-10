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
    private final IdMapper originalMapper;
    private final IdMapper prefixedMapper;
    private final String entityName;
    private final String auditEntityName;

    public MiddleIdData(AuditEntitiesConfiguration verEntCfg, IdMappingData mappingData, String prefix,
                        String entityName, boolean audited) {
        this.originalMapper = mappingData.getIdMapper();
        this.prefixedMapper = mappingData.getIdMapper().prefixMappedProperties(prefix);
        this.entityName = entityName;
        this.auditEntityName = audited ? verEntCfg.getAuditEntityName(entityName) : null;
    }

    /**
     * @return Original id mapper of the related entity.
     */
    public IdMapper getOriginalMapper() {
        return originalMapper;
    }

    /**
     * @return prefixed id mapper (with the names for the id fields that are used in the middle table) of the related entity.
     */
    public IdMapper getPrefixedMapper() {
        return prefixedMapper;
    }

    /**
     * @return Name of the related entity (regular, not audited).
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * @return Audit name of the related entity.
     */
    public String getAuditEntityName() {
        return auditEntityName;
    }

    /**
     * @return Is the entity, to which this middle id data correspond, audited.
     */
    public boolean isAudited() {
        return auditEntityName != null;
    }
}
