/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.query;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.internal.entities.mapper.id.AbstractCompositeIdMapper;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.internal.util.StringHelper;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.DEL_REVISION_TYPE_PARAMETER;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS_DEF_AUD_STR;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
 * Selects data from an audit entity.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public final class OneAuditEntityQueryGenerator extends AbstractRelationQueryGenerator {
	private final String mappedBy;
	private final boolean multipleIdMapperKey;
	private final MiddleIdData referencedIdData;

	public OneAuditEntityQueryGenerator(
			Configuration configuration,
			MiddleIdData referencingIdData,
			String referencedEntityName,
			MiddleIdData referencedIdData,
			boolean revisionTypeInId,
			String mappedBy,
			boolean mappedByKey,
			String orderByCollectionRole) {
		super(
				configuration,
				configuration.getAuditEntityName( referencedEntityName ),
				referencingIdData,
				revisionTypeInId,
				orderByCollectionRole
		);

		this.mappedBy = mappedBy;
		this.referencedIdData = referencedIdData;

		// HHH-11770 We use AbstractCompositeIdMapper here to handle EmbeddedIdMapper and MultipleIdMappper support
		// so that OneAuditEntityQueryGenerator supports mappings to both @IdClass and @EmbeddedId components.
		multipleIdMapperKey = (referencedIdData.getOriginalMapper() instanceof AbstractCompositeIdMapper) && mappedByKey;

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
	}

	@Override
	protected QueryBuilder buildQueryBuilderCommon(SessionFactoryImplementor sessionFactory) {
		// SELECT e FROM versionsEntity e
		final QueryBuilder qb = new QueryBuilder( entityName, REFERENCED_ENTITY_ALIAS, sessionFactory );
		qb.addProjection( null, REFERENCED_ENTITY_ALIAS, null, false );
		// WHERE
		if ( multipleIdMapperKey ) {
			// HHH-7625
			// support @OneToMany(mappedBy) to @ManyToOne @IdClass attribute.
			// e.originalId.id_ref_ed.id = :id_ref_ed
			final IdMapper mapper = getMultipleIdPrefixedMapper();
			mapper.addNamedIdEqualsToQuery( qb.getRootParameters(), null, referencingIdData.getPrefixedMapper(), true );
		}
		else {
			// e.id_ref_ed = :id_ref_ed
			referencingIdData.getPrefixedMapper().addNamedIdEqualsToQuery( qb.getRootParameters(), null, true );
		}

		// ORDER BY
		if ( !StringHelper.isEmpty( orderByCollectionRole ) ) {
			qb.addOrderFragment( REFERENCED_ENTITY_ALIAS, orderByCollectionRole );
		}

		return qb;
	}

	@Override
	protected void applyValidPredicates(QueryBuilder qb, Parameters rootParameters, boolean inclusive) {
		final String revisionPropertyPath = configuration.getRevisionNumberPath();
		// (selecting e entities at revision :revision)
		// --> based on auditStrategy (see above)
		auditStrategy.addEntityAtRevisionRestriction(
				configuration,
				qb,
				rootParameters,
				revisionPropertyPath,
				configuration.getRevisionEndFieldName(),
				true,
				referencedIdData,
				revisionPropertyPath,
				configuration.getOriginalIdPropertyName(),
				REFERENCED_ENTITY_ALIAS,
				REFERENCED_ENTITY_ALIAS_DEF_AUD_STR,
				true
		);
		// e.revision_type != DEL
		rootParameters.addWhereWithNamedParam( getRevisionTypePath(), true, "!=", DEL_REVISION_TYPE_PARAMETER );
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
		// e.revision = :revision
		removed.addWhereWithNamedParam( configuration.getRevisionNumberPath(), false, "=", REVISION_PARAMETER );
		// e.revision_type = DEL
		removed.addWhereWithNamedParam( getRevisionTypePath(), false, "=", DEL_REVISION_TYPE_PARAMETER );
	}

	private IdMapper getMultipleIdPrefixedMapper() {
		final String prefix = configuration.getOriginalIdPropertyName() + "." + mappedBy + ".";
		return referencingIdData.getOriginalMapper().prefixMappedProperties( prefix );
	}
}
