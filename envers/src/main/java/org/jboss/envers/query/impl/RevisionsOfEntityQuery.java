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

import org.jboss.envers.exception.VersionsException;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.query.criteria.VersionsCriterion;
import org.jboss.envers.RevisionType;
import org.jboss.envers.configuration.VersionsEntitiesConfiguration;
import org.jboss.envers.configuration.VersionsConfiguration;
import org.hibernate.proxy.HibernateProxy;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionsOfEntityQuery extends AbstractVersionsQuery {
    private final boolean selectEntitiesOnly;
    private final boolean selectDeletedEntities;

    public RevisionsOfEntityQuery(VersionsConfiguration verCfg,
                                  VersionsReaderImplementor versionsReader,
                                  Class<?> cls, boolean selectEntitiesOnly,
                                  boolean selectDeletedEntities) {
        super(verCfg, versionsReader, cls);

        this.selectEntitiesOnly = selectEntitiesOnly;
        this.selectDeletedEntities = selectDeletedEntities;
    }

    private Number getRevisionNumber(Map versionsEntity) {
        VersionsEntitiesConfiguration verEntCfg = verCfg.getVerEntCfg();

        String originalId = verEntCfg.getOriginalIdPropName();
        String revisionPropertyName = verEntCfg.getRevisionPropName();

        Object revisionInfoObject = ((Map) versionsEntity.get(originalId)).get(revisionPropertyName);

        if (revisionInfoObject instanceof HibernateProxy) {
            return (Number) ((HibernateProxy) revisionInfoObject).getHibernateLazyInitializer().getIdentifier();
        } else {
            // Not a proxy - must be read from cache or with a join
            return verCfg.getRevisionInfoNumberReader().getRevisionNumber(revisionInfoObject);   
        }
    }

    @SuppressWarnings({"unchecked"})
    public List list() throws VersionsException {
        VersionsEntitiesConfiguration verEntCfg = verCfg.getVerEntCfg();

        /*
        The query that should be executed in the versions table:
        SELECT e (unless another projection is specified) FROM ent_ver e, rev_entity r WHERE
          e.revision_type != DEL (if selectDeletedEntities == false) AND
          e.revision = r.revision AND
          (all specified conditions, transformed, on the "e" entity)
          ORDER BY e.revision ASC (unless another order or projection is specified)
         */      
        if (!selectDeletedEntities) {
            // e.revision_type != DEL AND
            qb.getRootParameters().addWhereWithParam(verEntCfg.getRevisionTypePropName(), "<>", RevisionType.DEL);
        }

        // all specified conditions, transformed
        for (VersionsCriterion criterion : criterions) {
            criterion.addToQuery(verCfg, entityName, qb, qb.getRootParameters());
        }

        if (!hasProjection && !hasOrder) {
            String revisionPropertyPath = verEntCfg.getRevisionPropPath();
            qb.addOrder(revisionPropertyPath, true);
        }

        if (!selectEntitiesOnly) {
            qb.addFrom(verCfg.getVerEntCfg().getRevisionInfoEntityName(), "r");
            qb.getRootParameters().addWhere(verCfg.getVerEntCfg().getRevisionPropPath(), true, "=", "r.id", false);
        }

        List<Object> queryResult = buildAndExecuteQuery();
        if (hasProjection) {
            return queryResult;
        } else {
            List entities = new ArrayList();
            String revisionTypePropertyName = verEntCfg.getRevisionTypePropName();

            for (Object resultRow : queryResult) {
                Map versionsEntity;
                Object revisionData;

                if (selectEntitiesOnly) {
                    versionsEntity = (Map) resultRow;
                    revisionData = null;
                } else {
                    Object[] arrayResultRow = (Object[]) resultRow;
                    versionsEntity = (Map) arrayResultRow[0];
                    revisionData = arrayResultRow[1];
                }

                Number revision = getRevisionNumber(versionsEntity);
                
                Object entity = entityInstantiator.createInstanceFromVersionsEntity(entityName, versionsEntity, revision);

                if (!selectEntitiesOnly) {
                    entities.add(new Object[] { entity, revisionData, versionsEntity.get(revisionTypePropertyName) });
                } else {
                    entities.add(entity);
                }
            }

            return entities;
        }
    }
}
