/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.query;

import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.id.MultipleIdMapper;
import org.hibernate.envers.boot.spi.AuditMetadataBuildingOptions;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;

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
	private final String queryString;
	private final String queryRemovedString;
	private final String mappedBy;
	private final boolean multipleIdMapperKey;

	public OneAuditEntityQueryGenerator(
			AuditMetadataBuildingOptions options,
			MiddleIdData referencingIdData,
			String referencedEntityName,
			MiddleIdData referencedIdData,
			boolean revisionTypeInId,
			String mappedBy,
			boolean mappedByKey) {
		super( options, referencingIdData, revisionTypeInId );

		this.mappedBy = mappedBy;

		if ( ( referencedIdData.getOriginalMapper() instanceof MultipleIdMapper ) && mappedByKey ) {
			multipleIdMapperKey = true;
		}
		else {
			multipleIdMapperKey = false;
		}

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
		final QueryBuilder commonPart = commonQueryPart( options.getAuditEntityName( referencedEntityName ) );
		final QueryBuilder validQuery = commonPart.deepCopy();
		final QueryBuilder removedQuery = commonPart.deepCopy();
		createValidDataRestrictions( options, referencedIdData, validQuery, validQuery.getRootParameters() );
		createValidAndRemovedDataRestrictions( options, referencedIdData, removedQuery );

		queryString = queryToString( validQuery );
		queryRemovedString = queryToString( removedQuery );
	}

	/**
	 * Compute common part for both queries.
	 */
	private QueryBuilder commonQueryPart(String versionsReferencedEntityName) {
		// SELECT e FROM versionsEntity e
		final QueryBuilder qb = new QueryBuilder( versionsReferencedEntityName, REFERENCED_ENTITY_ALIAS );
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
		return qb;
	}

	/**
	 * Creates query restrictions used to retrieve only actual data.
	 */
	private void createValidDataRestrictions(
			AuditMetadataBuildingOptions options,
			MiddleIdData referencedIdData,
			QueryBuilder qb,
			Parameters rootParameters) {
		final String revisionPropertyPath = options.getRevisionNumberPath();
		// (selecting e entities at revision :revision)
		// --> based on auditStrategy (see above)
		options.getAuditStrategy().addEntityAtRevisionRestriction(
				options,
				qb,
				rootParameters,
				revisionPropertyPath,
				options.getRevisionEndFieldName(),
				true,
				referencedIdData,
				revisionPropertyPath,
				options.getOriginalIdPropName(),
				REFERENCED_ENTITY_ALIAS,
				REFERENCED_ENTITY_ALIAS_DEF_AUD_STR,
				true
		);
		// e.revision_type != DEL
		rootParameters.addWhereWithNamedParam( getRevisionTypePath(), false, "!=", DEL_REVISION_TYPE_PARAMETER );
	}

	/**
	 * Create query restrictions used to retrieve actual data and deletions that took place at exactly given revision.
	 */
	private void createValidAndRemovedDataRestrictions(
			AuditMetadataBuildingOptions options,
			MiddleIdData referencedIdData,
			QueryBuilder remQb) {
		final Parameters disjoint = remQb.getRootParameters().addSubParameters( "or" );
		// Restrictions to match all valid rows.
		final Parameters valid = disjoint.addSubParameters( "and" );
		// Restrictions to match all rows deleted at exactly given revision.
		final Parameters removed = disjoint.addSubParameters( "and" );
		// Excluding current revision, because we need to match data valid at the previous one.
		createValidDataRestrictions( options, referencedIdData, remQb, valid );
		// e.revision = :revision
		removed.addWhereWithNamedParam( options.getRevisionNumberPath(), false, "=", REVISION_PARAMETER );
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

	private IdMapper getMultipleIdPrefixedMapper() {
		final String prefix = options.getOriginalIdPropName() + "." + mappedBy + ".";
		return referencingIdData.getOriginalMapper().prefixMappedProperties( prefix );
	}
}
