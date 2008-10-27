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
package org.jboss.envers.entities.mapper.relation.query;

import org.jboss.envers.entities.mapper.relation.MiddleIdData;
import org.jboss.envers.entities.mapper.relation.MiddleComponentData;
import org.jboss.envers.entities.mapper.id.QueryParameterData;
import org.jboss.envers.configuration.VersionsEntitiesConfiguration;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.RevisionType;
import org.jboss.envers.tools.query.QueryBuilder;
import org.jboss.envers.tools.query.Parameters;
import org.hibernate.Query;

import java.util.Collections;

/**
 * Selects data from a relation middle-table only.
 * @author Adam Warski (adam at warski dot org)
 */
public final class OneEntityQueryGenerator implements RelationQueryGenerator {
    private final String queryString;
    private final MiddleIdData referencingIdData;

    public OneEntityQueryGenerator(VersionsEntitiesConfiguration verEntCfg,
                                   String versionsMiddleEntityName,
                                   MiddleIdData referencingIdData,
                                   MiddleComponentData... componentDatas) {
        this.referencingIdData = referencingIdData;

        /*
         * The query that we need to create:
         *   SELECT new list(ee) FROM middleEntity ee WHERE
         * (only entities referenced by the association; id_ref_ing = id of the referencing entity)
         *     ee.originalId.id_ref_ing = :id_ref_ing AND
         * (the association at revision :revision)
         *     ee.revision = (SELECT max(ee2.revision) FROM middleEntity ee2
         *       WHERE ee2.revision <= :revision AND ee2.originalId.* = ee.originalId.*) AND
         * (only non-deleted entities and associations)
         *     ee.revision_type != DEL
         */
        String revisionPropertyPath = verEntCfg.getRevisionPropPath();
        String originalIdPropertyName = verEntCfg.getOriginalIdPropName();

        // SELECT new list(ee) FROM middleEntity ee
        QueryBuilder qb = new QueryBuilder(versionsMiddleEntityName, "ee");
        qb.addProjection("new list", "ee", false, false);
        // WHERE
        Parameters rootParameters = qb.getRootParameters();
        // ee.originalId.id_ref_ing = :id_ref_ing
        referencingIdData.getPrefixedMapper().addNamedIdEqualsToQuery(rootParameters, originalIdPropertyName, true);
        // SELECT max(ee2.revision) FROM middleEntity ee2
        QueryBuilder maxRevQb = qb.newSubQueryBuilder(versionsMiddleEntityName, "ee2");
        maxRevQb.addProjection("max", revisionPropertyPath, false);
        // WHERE
        Parameters maxRevQbParameters = maxRevQb.getRootParameters();
        // ee2.revision <= :revision
        maxRevQbParameters.addWhereWithNamedParam(revisionPropertyPath, "<=", "revision");
        // ee2.originalId.* = ee.originalId.*        
        String eeOriginalIdPropertyPath = "ee." + originalIdPropertyName;
        String ee2OriginalIdPropertyPath = "ee2." + originalIdPropertyName;
        referencingIdData.getPrefixedMapper().addIdsEqualToQuery(maxRevQbParameters, eeOriginalIdPropertyPath, ee2OriginalIdPropertyPath);
        for (MiddleComponentData componentData : componentDatas) {
            componentData.getComponentMapper().addMiddleEqualToQuery(maxRevQbParameters, eeOriginalIdPropertyPath, ee2OriginalIdPropertyPath);
        }
        // ee.revision = (SELECT max(...) ...)
        rootParameters.addWhere(revisionPropertyPath, "=", maxRevQb);       
        // ee.revision_type != DEL
        rootParameters.addWhereWithNamedParam(verEntCfg.getRevisionTypePropName(), "!=", "delrevisiontype");

        StringBuilder sb = new StringBuilder();
        qb.build(sb, Collections.<String, Object>emptyMap());
        queryString = sb.toString();
    }

    public Query getQuery(VersionsReaderImplementor versionsReader, Object primaryKey, Number revision) {
        Query query = versionsReader.getSession().createQuery(queryString);
        query.setParameter("revision", revision);
        query.setParameter("delrevisiontype", RevisionType.DEL);
        for (QueryParameterData paramData: referencingIdData.getPrefixedMapper().mapToQueryParametersFromId(primaryKey)) {
            paramData.setParameterValue(query);
        }

        return query;
    }
}
