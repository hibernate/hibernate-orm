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

import org.hibernate.envers.configuration.GlobalConfiguration;
import org.hibernate.envers.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.tools.query.Parameters;
import org.hibernate.envers.tools.query.QueryBuilder;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class QueryGeneratorTools {
    public static void addEntityAtRevision(GlobalConfiguration globalCfg, QueryBuilder qb, Parameters rootParameters,
                                           MiddleIdData idData, String revisionPropertyPath, String originalIdPropertyName,
                                           String alias1, String alias2) {
        // SELECT max(e.revision) FROM versionsReferencedEntity e2
        QueryBuilder maxERevQb = qb.newSubQueryBuilder(idData.getAuditEntityName(), alias2);
        maxERevQb.addProjection("max", revisionPropertyPath, false);
        // WHERE
        Parameters maxERevQbParameters = maxERevQb.getRootParameters();
        // e2.revision <= :revision
        maxERevQbParameters.addWhereWithNamedParam(revisionPropertyPath, "<=", "revision");
        // e2.id_ref_ed = e.id_ref_ed
        idData.getOriginalMapper().addIdsEqualToQuery(maxERevQbParameters,
                alias1 + "." + originalIdPropertyName, alias2 +"." + originalIdPropertyName);

        // e.revision = (SELECT max(...) ...)
        rootParameters.addWhere("e." + revisionPropertyPath, false, globalCfg.getCorrelatedSubqueryOperator(), maxERevQb);
    }

    public static void addAssociationAtRevision(QueryBuilder qb, Parameters rootParameters,
                                                MiddleIdData referencingIdData, String versionsMiddleEntityName,
                                                String eeOriginalIdPropertyPath, String revisionPropertyPath,
                                                String originalIdPropertyName, MiddleComponentData... componentDatas) {
        // SELECT max(ee2.revision) FROM middleEntity ee2
        QueryBuilder maxEeRevQb = qb.newSubQueryBuilder(versionsMiddleEntityName, "ee2");
        maxEeRevQb.addProjection("max", revisionPropertyPath, false);
        // WHERE
        Parameters maxEeRevQbParameters = maxEeRevQb.getRootParameters();
        // ee2.revision <= :revision
        maxEeRevQbParameters.addWhereWithNamedParam(revisionPropertyPath, "<=", "revision");
        // ee2.originalId.* = ee.originalId.*
        String ee2OriginalIdPropertyPath = "ee2." + originalIdPropertyName;
        referencingIdData.getPrefixedMapper().addIdsEqualToQuery(maxEeRevQbParameters, eeOriginalIdPropertyPath, ee2OriginalIdPropertyPath);
        for (MiddleComponentData componentData : componentDatas) {
            componentData.getComponentMapper().addMiddleEqualToQuery(maxEeRevQbParameters, eeOriginalIdPropertyPath, ee2OriginalIdPropertyPath);
        }

        // ee.revision = (SELECT max(...) ...)
        rootParameters.addWhere(revisionPropertyPath, "=", maxEeRevQb);
    }
}
