/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.query.impl;

import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.query.criteria.VersionsCriterion;
import org.jboss.envers.RevisionType;
import org.jboss.envers.tools.query.QueryBuilder;
import org.jboss.envers.configuration.VersionsEntitiesConfiguration;
import org.jboss.envers.configuration.VersionsConfiguration;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EntitiesAtRevisionQuery extends AbstractVersionsQuery {
    private final Number revision;

    public EntitiesAtRevisionQuery(VersionsConfiguration verCfg,
                                   VersionsReaderImplementor versionsReader, Class<?> cls,
                                   Number revision) {
        super(verCfg, versionsReader, cls);
        this.revision = revision;
    }

    @SuppressWarnings({"unchecked"})
    public List list() {
        /*
        The query that should be executed in the versions table:
        SELECT e FROM ent_ver e WHERE
          (all specified conditions, transformed, on the "e" entity) AND
          e.revision_type != DEL AND
          e.revision = (SELECT max(e2.revision) FROM ent_ver e2 WHERE
            e2.revision <= :revision AND e2.originalId.id = e.originalId.id)
         */

        QueryBuilder maxRevQb = qb.newSubQueryBuilder(versionsEntityName, "e2");

        VersionsEntitiesConfiguration verEntCfg = verCfg.getVerEntCfg();

        String revisionPropertyPath = verEntCfg.getRevisionPropPath();
        String originalIdPropertyName = verEntCfg.getOriginalIdPropName();

        // SELECT max(e2.revision)
        maxRevQb.addProjection("max", revisionPropertyPath, false);
        // e2.revision <= :revision
        maxRevQb.getRootParameters().addWhereWithParam(revisionPropertyPath, "<=", revision);
        // e2.id = e.id
        verCfg.getEntCfg().get(entityName).getIdMapper().addIdsEqualToQuery(maxRevQb.getRootParameters(),
                "e." + originalIdPropertyName, "e2." + originalIdPropertyName);

        // e.revision_type != DEL AND
        qb.getRootParameters().addWhereWithParam(verEntCfg.getRevisionTypePropName(), "<>", RevisionType.DEL);
        // e.revision = (SELECT max(...) ...)
        qb.getRootParameters().addWhere(revisionPropertyPath, verCfg.getGlobalCfg().getCorrelatedSubqueryOperator(), maxRevQb);
        // all specified conditions
        for (VersionsCriterion criterion : criterions) {
            criterion.addToQuery(verCfg, entityName, qb, qb.getRootParameters());
        }

        List queryResult = buildAndExecuteQuery();

        if (hasProjection) {
            return queryResult;
        } else {
            List result = new ArrayList();
            entityInstantiator.addInstancesFromVersionsEntities(entityName, result, queryResult, revision);

            return result;
        }
    }
}
