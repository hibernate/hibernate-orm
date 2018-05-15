/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.impl;

import java.util.Collection;
import java.util.List;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
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
			AuditReaderImplementor versionsReader,
			Class<?> cls,
			Number revision,
			boolean includeDeletions) {
		super( versionsReader, cls );
		this.revision = revision;
		this.includeDeletions = includeDeletions;
	}

	public EntitiesAtRevisionQuery(
			AuditReaderImplementor versionsReader,
			Class<?> cls,
			String entityName,
			Number revision,
			boolean includeDeletions) {
		super( versionsReader, cls, entityName );
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
		String revisionPropertyPath = getOptions().getRevisionNumberPath();
		String originalIdPropertyName = getOptions().getOriginalIdPropName();

		MiddleIdData referencedIdData = new MiddleIdData(
				getEntityConfiguration().getIdMappingData(),
				null,
				getEntityName(),
				getAuditEntityName()
		);

		// (selecting e entities at revision :revision)
		// --> based on auditStrategy (see above)
		getOptions().getAuditStrategy().addEntityAtRevisionRestriction(
				getOptions(),
				getQueryBuilder(),
				getQueryBuilder().getRootParameters(),
				revisionPropertyPath,
				getOptions().getRevisionEndFieldName(),
				true,
				referencedIdData,
				revisionPropertyPath,
				originalIdPropertyName,
				REFERENCED_ENTITY_ALIAS,
				REFERENCED_ENTITY_ALIAS_DEF_AUD_STR,
				true
		);

		if ( !includeDeletions ) {
			// e.revision_type != DEL
			getQueryBuilder().getRootParameters().addWhereWithParam(
					getOptions().getRevisionTypePropName(),
					"<>",
					RevisionType.DEL
			);
		}

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
