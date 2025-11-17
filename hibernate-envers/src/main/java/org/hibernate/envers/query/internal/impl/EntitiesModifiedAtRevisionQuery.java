/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.internal.impl;

import java.util.Collection;
import java.util.List;

import jakarta.persistence.criteria.JoinType;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.AuditAssociationQuery;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.query.Query;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
 * In comparison to {@link EntitiesAtRevisionQuery} this query returns an empty collection if an entity
 * of a certain type has not been changed in a given revision.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 *
 * @see EntitiesAtRevisionQuery
 */
public class EntitiesModifiedAtRevisionQuery extends AbstractAuditQuery {
	private final Number revision;

	public EntitiesModifiedAtRevisionQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Class<?> cls,
			Number revision) {
		super( enversService, versionsReader, cls );
		this.revision = revision;
	}

	public EntitiesModifiedAtRevisionQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Class<?> cls,
			String entityName,
			Number revision) {
		super( enversService, versionsReader, cls, entityName );
		this.revision = revision;
	}

	@Override
	public List list() {
		/*
		 * The query that we need to create:
		 *   SELECT new list(e) FROM versionsReferencedEntity e
		 *   WHERE
		 * (all specified conditions, transformed, on the "e" entity) AND
		 * e.revision = :revision
		 */
		String revisionPropertyPath = enversService.getConfig().getRevisionNumberPath();
		qb.getRootParameters().addWhereWithParam( revisionPropertyPath, "=", revision );

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
					null
			);
			addAssociationQuery( associationName, query );
		}
		return query;
	}
}
