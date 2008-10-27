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

import org.jboss.envers.tools.query.QueryBuilder;
import org.jboss.envers.tools.query.Parameters;
import org.jboss.envers.entities.mapper.relation.MiddleIdData;
import org.jboss.envers.entities.mapper.relation.MiddleComponentData;
import org.jboss.envers.configuration.GlobalConfiguration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class QueryGeneratorTools {
    public static void addEntityAtRevision(GlobalConfiguration globalCfg, QueryBuilder qb, Parameters rootParameters,
                                           MiddleIdData idData, String revisionPropertyPath, String originalIdPropertyName,
                                           String alias1, String alias2) {
        // SELECT max(e.revision) FROM versionsReferencedEntity e2
        QueryBuilder maxERevQb = qb.newSubQueryBuilder(idData.getVersionsEntityName(), alias2);
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
