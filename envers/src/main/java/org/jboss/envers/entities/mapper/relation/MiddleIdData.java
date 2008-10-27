/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 *
 * See the copyright.txt in the distribution for a  full listing of individual
 * contributors. This copyrighted material is made available to anyone wishing
 * to use,  modify, copy, or redistribute it subject to the terms and
 * conditions of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT A WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.relation;

import org.jboss.envers.entities.mapper.id.IdMapper;
import org.jboss.envers.entities.IdMappingData;
import org.jboss.envers.configuration.VersionsEntitiesConfiguration;

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

    public MiddleIdData(VersionsEntitiesConfiguration verEntCfg, IdMappingData mappingData, String prefix,
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
