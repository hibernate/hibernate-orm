/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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

import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.strategy.AuditStrategy;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.DEL_REVISION_TYPE_PARAMETER;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.MIDDLE_ENTITY_ALIAS;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
 * Selects data from a relation middle-table only.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public final class OneEntityQueryGenerator extends AbstractRelationQueryGenerator {
	private final String queryString;
	private final String queryRemovedString;

	public OneEntityQueryGenerator(
			AuditEntitiesConfiguration verEntCfg, AuditStrategy auditStrategy,
			String versionsMiddleEntityName, MiddleIdData referencingIdData,
			boolean revisionTypeInId, MiddleComponentData... componentData) {
		super( verEntCfg, referencingIdData, revisionTypeInId );

		/*
		 * The valid query that we need to create:
		 *   SELECT ee FROM middleEntity ee WHERE
		 * (only entities referenced by the association; id_ref_ing = id of the referencing entity)
		 *     ee.originalId.id_ref_ing = :id_ref_ing AND
		 *
		 * (the association at revision :revision)
		 *   --> for DefaultAuditStrategy:
		 *     ee.revision = (SELECT max(ee2.revision) FROM middleEntity ee2
		 *       WHERE ee2.revision <= :revision AND ee2.originalId.* = ee.originalId.*)
		 *
		 *   --> for ValidityAuditStrategy:
		 *     ee.revision <= :revision and (ee.endRevision > :revision or ee.endRevision is null)
		 *
		 *     AND
		 *
		 * (only non-deleted entities and associations)
		 *     ee.revision_type != DEL
		 */
		final QueryBuilder commonPart = commonQueryPart( versionsMiddleEntityName );
		final QueryBuilder validQuery = commonPart.deepCopy();
		final QueryBuilder removedQuery = commonPart.deepCopy();
		createValidDataRestrictions(
				auditStrategy, versionsMiddleEntityName, validQuery, validQuery.getRootParameters(), true, componentData
		);
		createValidAndRemovedDataRestrictions( auditStrategy, versionsMiddleEntityName, removedQuery, componentData );

		queryString = queryToString( validQuery );
		queryRemovedString = queryToString( removedQuery );
	}

	/**
	 * Compute common part for both queries.
	 */
	private QueryBuilder commonQueryPart(String versionsMiddleEntityName) {
		// SELECT ee FROM middleEntity ee
		final QueryBuilder qb = new QueryBuilder( versionsMiddleEntityName, MIDDLE_ENTITY_ALIAS );
		qb.addProjection( null, MIDDLE_ENTITY_ALIAS, false, false );
		// WHERE
		// ee.originalId.id_ref_ing = :id_ref_ing
		referencingIdData.getPrefixedMapper().addNamedIdEqualsToQuery(
				qb.getRootParameters(),
				verEntCfg.getOriginalIdPropName(),
				true
		);
		return qb;
	}

	/**
	 * Creates query restrictions used to retrieve only actual data.
	 */
	private void createValidDataRestrictions(
			AuditStrategy auditStrategy, String versionsMiddleEntityName,
			QueryBuilder qb, Parameters rootParameters, boolean inclusive,
			MiddleComponentData... componentData) {
		final String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
		final String originalIdPropertyName = verEntCfg.getOriginalIdPropName();
		final String eeOriginalIdPropertyPath = MIDDLE_ENTITY_ALIAS + "." + originalIdPropertyName;
		// (with ee association at revision :revision)
		// --> based on auditStrategy (see above)
		auditStrategy.addAssociationAtRevisionRestriction(
				qb, rootParameters, revisionPropertyPath, verEntCfg.getRevisionEndFieldName(), true,
				referencingIdData, versionsMiddleEntityName, eeOriginalIdPropertyPath, revisionPropertyPath,
				originalIdPropertyName, MIDDLE_ENTITY_ALIAS, inclusive, componentData
		);
		// ee.revision_type != DEL
		rootParameters.addWhereWithNamedParam( getRevisionTypePath(), "!=", DEL_REVISION_TYPE_PARAMETER );
	}

	/**
	 * Create query restrictions used to retrieve actual data and deletions that took place at exactly given revision.
	 */
	private void createValidAndRemovedDataRestrictions(
			AuditStrategy auditStrategy, String versionsMiddleEntityName,
			QueryBuilder remQb, MiddleComponentData... componentData) {
		final Parameters disjoint = remQb.getRootParameters().addSubParameters( "or" );
		// Restrictions to match all valid rows.
		final Parameters valid = disjoint.addSubParameters( "and" );
		// Restrictions to match all rows deleted at exactly given revision.
		final Parameters removed = disjoint.addSubParameters( "and" );
		// Excluding current revision, because we need to match data valid at the previous one.
		createValidDataRestrictions( auditStrategy, versionsMiddleEntityName, remQb, valid, false, componentData );
		// ee.revision = :revision
		removed.addWhereWithNamedParam( verEntCfg.getRevisionNumberPath(), "=", REVISION_PARAMETER );
		// ee.revision_type = DEL
		removed.addWhereWithNamedParam( getRevisionTypePath(), "=", DEL_REVISION_TYPE_PARAMETER );
	}

	@Override
	protected String getQueryString() {
		return queryString;
	}

	@Override
	protected String getQueryRemovedString() {
		return queryRemovedString;
	}
}
