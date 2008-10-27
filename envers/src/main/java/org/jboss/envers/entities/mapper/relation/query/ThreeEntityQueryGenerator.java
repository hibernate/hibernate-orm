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

import org.jboss.envers.entities.mapper.id.QueryParameterData;
import org.jboss.envers.entities.mapper.relation.MiddleIdData;
import org.jboss.envers.entities.mapper.relation.MiddleComponentData;
import org.jboss.envers.configuration.VersionsEntitiesConfiguration;
import org.jboss.envers.configuration.GlobalConfiguration;
import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.RevisionType;
import org.jboss.envers.tools.query.QueryBuilder;
import org.jboss.envers.tools.query.Parameters;
import org.hibernate.Query;

import java.util.Collections;

/**
 * Selects data from a relation middle-table and a two related versions entity.
 * @author Adam Warski (adam at warski dot org)
 */
public final class ThreeEntityQueryGenerator implements RelationQueryGenerator {
    private final String queryString;
    private final MiddleIdData referencingIdData;

    public ThreeEntityQueryGenerator(GlobalConfiguration globalCfg,
                                     VersionsEntitiesConfiguration verEntCfg,
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
        String revisionPropertyPath = verEntCfg.getRevisionPropPath();
        String originalIdPropertyName = verEntCfg.getOriginalIdPropName();

        String eeOriginalIdPropertyPath = "ee." + originalIdPropertyName;

        // SELECT new list(ee) FROM middleEntity ee
        QueryBuilder qb = new QueryBuilder(versionsMiddleEntityName, "ee");
        qb.addFrom(referencedIdData.getVersionsEntityName(), "e");
        qb.addFrom(indexIdData.getVersionsEntityName(), "f");
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
