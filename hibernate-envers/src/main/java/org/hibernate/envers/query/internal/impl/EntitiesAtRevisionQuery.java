/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.internal.impl;

import java.util.Collection;
import java.util.List;

import jakarta.persistence.criteria.JoinType;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.AuditAssociationQuery;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.query.Query;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS_DEF_AUD_STR;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Chris Cranford
 */
public class EntitiesAtRevisionQuery extends AbstractAuditQuery {
	private final Number revision;
	private final boolean includeDeletions;

	public EntitiesAtRevisionQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Class<?> cls,
			Number revision,
			boolean includeDeletions) {
		super( enversService, versionsReader, cls );
		this.revision = revision;
		this.includeDeletions = includeDeletions;
	}

	public EntitiesAtRevisionQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader, Class<?> cls,
			String entityName, Number revision, boolean includeDeletions) {
		super( enversService, versionsReader, cls, entityName );
		this.revision = revision;
		this.includeDeletions = includeDeletions;
	}

	public List list() {
		/*
		 * The query that we need to create:
		 *   SELECT new list(e) FROM versionsReferencedEntity e
		 *   WHERE
		 * (all specified conditions, transformed, on the "e" entity) AND
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
		Configuration configuration = enversService.getConfig();
		String revisionPropertyPath = configuration.getRevisionNumberPath();
		String originalIdPropertyName = configuration.getOriginalIdPropertyName();

		MiddleIdData referencedIdData = new MiddleIdData(
				configuration,
				enversService.getEntitiesConfigurations().get( entityName ).getIdMappingData(),
				null,
				entityName,
				enversService.getEntitiesConfigurations().isVersioned( entityName )
		);

		// (selecting e entities at revision :revision)
		// --> based on auditStrategy (see above)
		enversService.getAuditStrategy().addEntityAtRevisionRestriction(
				configuration,
				qb,
				qb.getRootParameters(),
				revisionPropertyPath,
				configuration.getRevisionEndFieldName(),
				true,
				referencedIdData,
				revisionPropertyPath,
				originalIdPropertyName,
				REFERENCED_ENTITY_ALIAS,
				REFERENCED_ENTITY_ALIAS_DEF_AUD_STR,
				true
		);

		if ( configuration.isFindByRevisionExactMatch() ) {
			// When EnversSettings#FIND_BY_REVISION_EXACT_MATCH is true, this forces this condition
			qb.getRootParameters().addWhereWithNamedParam( revisionPropertyPath, "=", REVISION_PARAMETER );
		}

		if ( !includeDeletions ) {
			// e.revision_type != DEL
			qb.getRootParameters().addWhereWithParam( configuration.getRevisionTypePropertyName(), "<>", RevisionType.DEL );
		}

		// all specified conditions
		for ( AuditCriterion criterion : criterions ) {
			criterion.addToQuery(
					enversService,
					versionsReader,
					aliasToEntityNameMap,
					aliasToComponentPropertyNameMap,
					QueryConstants.REFERENCED_ENTITY_ALIAS,
					qb,
					qb.getRootParameters()
			);
		}

		for ( AbstractAuditAssociationQuery<?> associationQuery : associationQueries ) {
			associationQuery.addCriterionToQuery( versionsReader );
		}

		Query query = buildQuery();
		// add named parameter (used for ValidityAuditStrategy and association queries)
		Collection<String> params = query.getParameterMetadata().getNamedParameterNames();
		if ( params.contains( REVISION_PARAMETER ) ) {
			query.setParameter( REVISION_PARAMETER, revision );
		}
		List queryResult = query.list();
		return applyProjections( queryResult, revision );
	}

	@Override
	public AuditAssociationQuery<? extends AuditQuery> traverseRelation(
			String associationName,
			JoinType joinType,
			String alias,
			AuditCriterion onClauseCriterion) {
		AbstractAuditAssociationQuery<AuditQueryImplementor> query = associationQueryMap.get( associationName );
		if ( query == null ) {
			query = new EntitiesAtRevisionAssociationQuery<>(
					enversService,
					versionsReader,
					this,
					qb,
					associationName,
					joinType,
					aliasToEntityNameMap,
					aliasToComponentPropertyNameMap,
					REFERENCED_ENTITY_ALIAS,
					alias,
					onClauseCriterion
			);
			addAssociationQuery( associationName, query );
		}
		return query;
	}
}
