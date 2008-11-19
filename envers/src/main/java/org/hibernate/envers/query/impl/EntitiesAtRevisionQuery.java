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
package org.hibernate.envers.query.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.configuration.AuditEntitiesConfiguration;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.query.QueryBuilder;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EntitiesAtRevisionQuery extends AbstractAuditQuery {
    private final Number revision;

    public EntitiesAtRevisionQuery(AuditConfiguration verCfg,
                                   AuditReaderImplementor versionsReader, Class<?> cls,
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

        AuditEntitiesConfiguration verEntCfg = verCfg.getAuditEntCfg();

        String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
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
        for (AuditCriterion criterion : criterions) {
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
