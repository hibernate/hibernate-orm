/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.query;

import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.strategy.AuditStrategy;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.DEL_REVISION_TYPE_PARAMETER;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS_DEF_AUD_STR;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
 * Selects data from an audit entity.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public final class OneAuditEntityQueryGenerator extends AbstractRelationQueryGenerator {
	private final String queryString;
	private final String queryRemovedString;

	public OneAuditEntityQueryGenerator(
			GlobalConfiguration globalCfg, AuditEntitiesConfiguration verEntCfg,
			AuditStrategy auditStrategy, MiddleIdData referencingIdData,
			String referencedEntityName, MiddleIdData referencedIdData, boolean revisionTypeInId) {
		super( verEntCfg, referencingIdData, revisionTypeInId );

		/*
		 * The valid query that we need to create:
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
		final QueryBuilder commonPart = commonQueryPart( verEntCfg.getAuditEntityName( referencedEntityName ) );
		final QueryBuilder validQuery = commonPart.deepCopy();
		final QueryBuilder removedQuery = commonPart.deepCopy();
		createValidDataRestrictions(
				globalCfg, auditStrategy, referencedIdData, validQuery, validQuery.getRootParameters()
		);
		createValidAndRemovedDataRestrictions( globalCfg, auditStrategy, referencedIdData, removedQuery );

		queryString = queryToString( validQuery );
		queryRemovedString = queryToString( removedQuery );
	}

	/**
	 * Compute common part for both queries.
	 */
	private QueryBuilder commonQueryPart(String versionsReferencedEntityName) {
		// SELECT e FROM versionsEntity e
		final QueryBuilder qb = new QueryBuilder( versionsReferencedEntityName, REFERENCED_ENTITY_ALIAS );
		qb.addProjection( null, REFERENCED_ENTITY_ALIAS, false, false );
		// WHERE
		// e.id_ref_ed = :id_ref_ed
		referencingIdData.getPrefixedMapper().addNamedIdEqualsToQuery( qb.getRootParameters(), null, true );
		return qb;
	}

	/**
	 * Creates query restrictions used to retrieve only actual data.
	 */
	private void createValidDataRestrictions(
			GlobalConfiguration globalCfg, AuditStrategy auditStrategy,
			MiddleIdData referencedIdData, QueryBuilder qb, Parameters rootParameters) {
		final String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
		// (selecting e entities at revision :revision)
		// --> based on auditStrategy (see above)
		auditStrategy.addEntityAtRevisionRestriction(
				globalCfg, qb, rootParameters, revisionPropertyPath,
				verEntCfg.getRevisionEndFieldName(), true, referencedIdData, revisionPropertyPath,
				verEntCfg.getOriginalIdPropName(), REFERENCED_ENTITY_ALIAS, REFERENCED_ENTITY_ALIAS_DEF_AUD_STR,
				true
		);
		// e.revision_type != DEL
		rootParameters.addWhereWithNamedParam( getRevisionTypePath(), false, "!=", DEL_REVISION_TYPE_PARAMETER );
	}

	/**
	 * Create query restrictions used to retrieve actual data and deletions that took place at exactly given revision.
	 */
	private void createValidAndRemovedDataRestrictions(
			GlobalConfiguration globalCfg, AuditStrategy auditStrategy,
			MiddleIdData referencedIdData, QueryBuilder remQb) {
		final Parameters disjoint = remQb.getRootParameters().addSubParameters( "or" );
		// Restrictions to match all valid rows.
		final Parameters valid = disjoint.addSubParameters( "and" );
		// Restrictions to match all rows deleted at exactly given revision.
		final Parameters removed = disjoint.addSubParameters( "and" );
		// Excluding current revision, because we need to match data valid at the previous one.
		createValidDataRestrictions( globalCfg, auditStrategy, referencedIdData, remQb, valid );
		// e.revision = :revision
		removed.addWhereWithNamedParam( verEntCfg.getRevisionNumberPath(), false, "=", REVISION_PARAMETER );
		// e.revision_type = DEL
		removed.addWhereWithNamedParam( getRevisionTypePath(), false, "=", DEL_REVISION_TYPE_PARAMETER );
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
