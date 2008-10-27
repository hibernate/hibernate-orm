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

import org.jboss.envers.configuration.VersionsEntitiesConfiguration;
import org.jboss.envers.entities.mapper.relation.query.RelationQueryGenerator;

/**
 * Data that is used by all collection mappers, regardless of the type.  
 * @author Adam Warski (adam at warski dot org)
 */
public final class CommonCollectionMapperData {
    private final VersionsEntitiesConfiguration verEntCfg;
    private final String versionsMiddleEntityName;
    private final String collectionReferencingPropertyName;
    private final MiddleIdData referencingIdData;
    private final RelationQueryGenerator queryGenerator;

    public CommonCollectionMapperData(VersionsEntitiesConfiguration verEntCfg, String versionsMiddleEntityName,
                                      String collectionReferencingPropertyName, MiddleIdData referencingIdData,
                                      RelationQueryGenerator queryGenerator) {
        this.verEntCfg = verEntCfg;
        this.versionsMiddleEntityName = versionsMiddleEntityName;
        this.collectionReferencingPropertyName = collectionReferencingPropertyName;
        this.referencingIdData = referencingIdData;
        this.queryGenerator = queryGenerator;
    }

    public VersionsEntitiesConfiguration getVerEntCfg() {
        return verEntCfg;
    }

    public String getVersionsMiddleEntityName() {
        return versionsMiddleEntityName;
    }

    public String getCollectionReferencingPropertyName() {
        return collectionReferencingPropertyName;
    }

    public MiddleIdData getReferencingIdData() {
        return referencingIdData;
    }

    public RelationQueryGenerator getQueryGenerator() {
        return queryGenerator;
    }
}
