/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.impl;

import java.util.Collection;
import java.util.List;

import org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.query.Query;

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
			AuditReaderImplementor versionsReader,
			Class<?> cls,
			Number revision) {
		super( versionsReader, cls );
		this.revision = revision;
	}

	public EntitiesModifiedAtRevisionQuery(
			AuditReaderImplementor versionsReader,
			Class<?> cls,
			String entityName,
			Number revision) {
		super( versionsReader, cls, entityName );
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
		getQueryBuilder().getRootParameters().addWhereWithParam( getOptions().getRevisionNumberPath(), "=", revision );

		// all specified conditions
		applyCriterions( QueryConstants.REFERENCED_ENTITY_ALIAS );

		Query query = buildQuery();
		// add named parameter (used for ValidityAuditStrategy and association queries)
		Collection<String> params = query.getParameterMetadata().getNamedParameterNames();
		if ( params.contains( REVISION_PARAMETER ) ) {
			query.setParameter( REVISION_PARAMETER, revision );
		}
		List queryResult = query.list();
		return applyProjections( queryResult, revision );
	}
}
