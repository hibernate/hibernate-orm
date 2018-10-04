/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.query;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
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
 * @author Chris Cranford
 */
public final class OneEntityQueryGenerator extends AbstractRelationQueryGenerator {
	private final MiddleComponentData[] componentDatas;

	public OneEntityQueryGenerator(
			GlobalConfiguration globalCfg,
			AuditEntitiesConfiguration verEntCfg,
			AuditStrategy auditStrategy,
			String versionsMiddleEntityName,
			MiddleIdData referencingIdData,
			boolean revisionTypeInId,
			MiddleComponentData... componentData) {
		super(
				globalCfg,
				verEntCfg,
				auditStrategy,
				versionsMiddleEntityName,
				referencingIdData,
				revisionTypeInId,
				null
		);

		this.componentDatas = componentData;

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
	}

	@Override
	protected QueryBuilder buildQueryBuilderCommon(SessionFactoryImplementor sessionFactory) {
		// SELECT ee FROM middleEntity ee
		final QueryBuilder qb = new QueryBuilder( entityName, MIDDLE_ENTITY_ALIAS, sessionFactory );
		qb.addProjection( null, MIDDLE_ENTITY_ALIAS, null, false );
		// WHERE
		// ee.originalId.id_ref_ing = :id_ref_ing
		referencingIdData.getPrefixedMapper().addNamedIdEqualsToQuery(
				qb.getRootParameters(),
				verEntCfg.getOriginalIdPropName(),
				true
		);

		// NOTE:
		// No `orderBy` fragment is specified because this generator is used for
		// embeddables and enumerations where either a Set-based container will
		// force the SETORDINAL property to give us a unique primary key tuple
		// or an @IndexColumn/@OrderColumn must be specified that takes priority
		// over an @OrderBy fragment.

		return qb;
	}

	@Override
	protected void applyValidPredicates(QueryBuilder qb, Parameters rootParameters, boolean inclusive) {
		final String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
		final String originalIdPropertyName = verEntCfg.getOriginalIdPropName();
		final String eeOriginalIdPropertyPath = MIDDLE_ENTITY_ALIAS + "." + originalIdPropertyName;
		// (with ee association at revision :revision)
		// --> based on auditStrategy (see above)
		auditStrategy.addAssociationAtRevisionRestriction(
				qb,
				rootParameters,
				revisionPropertyPath,
				verEntCfg.getRevisionEndFieldName(),
				true,
				referencingIdData,
				entityName,
				eeOriginalIdPropertyPath,
				revisionPropertyPath,
				originalIdPropertyName,
				MIDDLE_ENTITY_ALIAS,
				inclusive,
				componentDatas
		);
		// ee.revision_type != DEL
		rootParameters.addWhereWithNamedParam( getRevisionTypePath(), "!=", DEL_REVISION_TYPE_PARAMETER );
	}

	@Override
	protected void applyValidAndRemovePredicates(QueryBuilder remQb) {
		final Parameters disjoint = remQb.getRootParameters().addSubParameters( "or" );
		// Restrictions to match all valid rows.
		final Parameters valid = disjoint.addSubParameters( "and" );
		// Restrictions to match all rows deleted at exactly given revision.
		final Parameters removed = disjoint.addSubParameters( "and" );
		// Excluding current revision, because we need to match data valid at the previous one.
		applyValidPredicates( remQb, valid, false );
		// ee.revision = :revision
		removed.addWhereWithNamedParam( verEntCfg.getRevisionNumberPath(), "=", REVISION_PARAMETER );
		// ee.revision_type = DEL
		removed.addWhereWithNamedParam( getRevisionTypePath(), "=", DEL_REVISION_TYPE_PARAMETER );
	}
}
