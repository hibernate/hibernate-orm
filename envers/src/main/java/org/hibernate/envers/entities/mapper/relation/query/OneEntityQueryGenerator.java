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
package org.hibernate.envers.entities.mapper.relation.query;

import java.util.Collections;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.AuditEntitiesConfiguration;
import org.hibernate.envers.entities.mapper.id.QueryParameterData;
import org.hibernate.envers.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.query.Parameters;
import org.hibernate.envers.tools.query.QueryBuilder;

import org.hibernate.Query;

/**
 * Selects data from a relation middle-table only.
 * @author Adam Warski (adam at warski dot org)
 */
public final class OneEntityQueryGenerator implements RelationQueryGenerator {
    private final String queryString;
    private final MiddleIdData referencingIdData;

    public OneEntityQueryGenerator(AuditEntitiesConfiguration verEntCfg,
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
        String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
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

    public Query getQuery(AuditReaderImplementor versionsReader, Object primaryKey, Number revision) {
        Query query = versionsReader.getSession().createQuery(queryString);
        query.setParameter("revision", revision);
        query.setParameter("delrevisiontype", RevisionType.DEL);
        for (QueryParameterData paramData: referencingIdData.getPrefixedMapper().mapToQueryParametersFromId(primaryKey)) {
            paramData.setParameterValue(query);
        }

        return query;
    }
}
