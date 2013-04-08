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
package org.hibernate.envers.internal.entities.mapper.relation.query;

import java.util.Collections;

import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.strategy.AuditStrategy;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.DEL_REVISION_TYPE_PARAMETER;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS_DEF_AUD_STR;

/**
 * Selects data from an audit entity.
 * @author Adam Warski (adam at warski dot org)
 */
public final class OneAuditEntityQueryGenerator extends AbstractRelationQueryGenerator {
    private final String queryString;

    public OneAuditEntityQueryGenerator(GlobalConfiguration globalCfg, AuditEntitiesConfiguration verEntCfg, 
                                        AuditStrategy auditStrategy,
                                        MiddleIdData referencingIdData,
                                        String referencedEntityName, MiddleIdData referencedIdData,
										boolean revisionTypeInId) {
		super( verEntCfg, referencingIdData, revisionTypeInId );

        /*
         * The query that we need to create:
         *   SELECT e FROM versionsReferencedEntity e
         *   WHERE
         * (only entities referenced by the association; id_ref_ing = id of the referencing entity)
         *     e.id_ref_ing = :id_ref_ing AND
         * (selecting e entities at revision :revision)
         *   --> for DefaultAuditStrategy:
         *     e.revision = (SELECT max(e2.revision) FROM versionsReferencedEntity e2
         *       WHERE e2.revision <= :revision AND e2.id = e.id) 
         *     
         *   --> for ValidityAuditStrategy:
         *     e.revision <= :revision and (e.endRevision > :revision or e.endRevision is null)
         *     
         *     AND
         * (only non-deleted entities)
         *     e.revision_type != DEL
         */
        String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
        String originalIdPropertyName = verEntCfg.getOriginalIdPropName();

        String versionsReferencedEntityName = verEntCfg.getAuditEntityName(referencedEntityName);

        // SELECT e FROM versionsEntity e
        QueryBuilder qb = new QueryBuilder(versionsReferencedEntityName, REFERENCED_ENTITY_ALIAS);
        qb.addProjection(null, REFERENCED_ENTITY_ALIAS, false, false);
        // WHERE
        Parameters rootParameters = qb.getRootParameters();
        // e.id_ref_ed = :id_ref_ed
        referencingIdData.getPrefixedMapper().addNamedIdEqualsToQuery(rootParameters, null, true);

        // (selecting e entities at revision :revision)
        // --> based on auditStrategy (see above)
        auditStrategy.addEntityAtRevisionRestriction(globalCfg, qb, revisionPropertyPath,
        		verEntCfg.getRevisionEndFieldName(), true, referencedIdData, 
				revisionPropertyPath, originalIdPropertyName, REFERENCED_ENTITY_ALIAS, REFERENCED_ENTITY_ALIAS_DEF_AUD_STR);

        // e.revision_type != DEL
        rootParameters.addWhereWithNamedParam(getRevisionTypePath(), false, "!=", DEL_REVISION_TYPE_PARAMETER);

        StringBuilder sb = new StringBuilder();
        qb.build(sb, Collections.<String, Object>emptyMap());
        queryString = sb.toString();
    }

	@Override
	protected String getQueryString() {
		return queryString;
	}
}
