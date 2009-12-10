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
import org.hibernate.envers.configuration.GlobalConfiguration;
import org.hibernate.envers.configuration.AuditEntitiesConfiguration;
import org.hibernate.envers.entities.mapper.id.QueryParameterData;
import org.hibernate.envers.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.query.Parameters;
import org.hibernate.envers.tools.query.QueryBuilder;

import org.hibernate.Query;

/**
 * Selects data from a relation middle-table and a two related versions entity.
 * @author Adam Warski (adam at warski dot org)
 */
public final class ThreeEntityQueryGenerator implements RelationQueryGenerator {
    private final String queryString;
    private final MiddleIdData referencingIdData;

    public ThreeEntityQueryGenerator(GlobalConfiguration globalCfg,
                                     AuditEntitiesConfiguration verEntCfg,
                                     String versionsMiddleEntityName,
                                     MiddleIdData referencingIdData,
                                     MiddleIdData referencedIdData,
                                     MiddleIdData indexIdData,
                                     MiddleComponentData... componentDatas) {
        this.referencingIdData = referencingIdData;

        /*
         * The query that we need to create:
         *   SELECT new list(ee, e, f) FROM versionsReferencedEntity e, versionsIndexEntity f, middleEntity ee
         *   WHERE
         * (entities referenced by the middle table; id_ref_ed = id of the referenced entity)
         *     ee.id_ref_ed = e.id_ref_ed AND
         * (entities referenced by the middle table; id_ref_ind = id of the index entity)
         *     ee.id_ref_ind = f.id_ref_ind AND
         * (only entities referenced by the association; id_ref_ing = id of the referencing entity)
         *     ee.id_ref_ing = :id_ref_ing AND
         * (selecting e entities at revision :revision)
         *     e.revision = (SELECT max(e2.revision) FROM versionsReferencedEntity e2
         *       WHERE e2.revision <= :revision AND e2.id_ref_ed = e.id_ref_ed) AND
         * (selecting f entities at revision :revision)
         *     f.revision = (SELECT max(f2.revision) FROM versionsIndexEntity f2
         *       WHERE f2.revision <= :revision AND f2.id_ref_ed = f.id_ref_ed) AND
         * (the association at revision :revision)
         *     ee.revision = (SELECT max(ee2.revision) FROM middleEntity ee2
         *       WHERE ee2.revision <= :revision AND ee2.originalId.* = ee.originalId.*) AND
         * (only non-deleted entities and associations)
         *     ee.revision_type != DEL AND
         *     e.revision_type != DEL AND
         *     f.revision_type != DEL
         */
        String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
        String originalIdPropertyName = verEntCfg.getOriginalIdPropName();

        String eeOriginalIdPropertyPath = "ee." + originalIdPropertyName;

        // SELECT new list(ee) FROM middleEntity ee
        QueryBuilder qb = new QueryBuilder(versionsMiddleEntityName, "ee");
        qb.addFrom(referencedIdData.getAuditEntityName(), "e");
        qb.addFrom(indexIdData.getAuditEntityName(), "f");
        qb.addProjection("new list", "ee, e, f", false, false);
        // WHERE
        Parameters rootParameters = qb.getRootParameters();
        // ee.id_ref_ed = e.id_ref_ed
        referencedIdData.getPrefixedMapper().addIdsEqualToQuery(rootParameters, eeOriginalIdPropertyPath,
                referencedIdData.getOriginalMapper(), "e." + originalIdPropertyName);
        // ee.id_ref_ind = f.id_ref_ind
        indexIdData.getPrefixedMapper().addIdsEqualToQuery(rootParameters, eeOriginalIdPropertyPath,
                indexIdData.getOriginalMapper(), "f." + originalIdPropertyName);
        // ee.originalId.id_ref_ing = :id_ref_ing
        referencingIdData.getPrefixedMapper().addNamedIdEqualsToQuery(rootParameters, originalIdPropertyName, true);

        // e.revision = (SELECT max(...) ...)
        QueryGeneratorTools.addEntityAtRevision(globalCfg, qb, rootParameters, referencedIdData, revisionPropertyPath,
                originalIdPropertyName, "e", "e2");

        // f.revision = (SELECT max(...) ...)
        QueryGeneratorTools.addEntityAtRevision(globalCfg, qb, rootParameters, indexIdData, revisionPropertyPath,
                originalIdPropertyName, "f", "f2");

        // ee.revision = (SELECT max(...) ...)
        QueryGeneratorTools.addAssociationAtRevision(qb, rootParameters, referencingIdData, versionsMiddleEntityName,
                eeOriginalIdPropertyPath, revisionPropertyPath, originalIdPropertyName, componentDatas);

        // ee.revision_type != DEL
        rootParameters.addWhereWithNamedParam(verEntCfg.getRevisionTypePropName(), "!=", "delrevisiontype");
        // e.revision_type != DEL
        rootParameters.addWhereWithNamedParam("e." + verEntCfg.getRevisionTypePropName(), false, "!=", "delrevisiontype");
        // f.revision_type != DEL
        rootParameters.addWhereWithNamedParam("f." + verEntCfg.getRevisionTypePropName(), false, "!=", "delrevisiontype");

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
