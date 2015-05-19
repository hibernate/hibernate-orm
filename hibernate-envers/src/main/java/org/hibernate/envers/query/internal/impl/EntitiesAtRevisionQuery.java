/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.criteria.AuditCriterion;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS_DEF_AUD_STR;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
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

	@SuppressWarnings({"unchecked"})
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
		AuditEntitiesConfiguration verEntCfg = enversService.getAuditEntitiesConfiguration();
		String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
		String originalIdPropertyName = verEntCfg.getOriginalIdPropName();

		MiddleIdData referencedIdData = new MiddleIdData(
				verEntCfg,
				enversService.getEntitiesConfigurations().get( entityName ).getIdMappingData(),
				null,
				entityName,
				enversService.getEntitiesConfigurations().isVersioned( entityName )
		);

		// (selecting e entities at revision :revision)
		// --> based on auditStrategy (see above)
		enversService.getAuditStrategy().addEntityAtRevisionRestriction(
				enversService.getGlobalConfiguration(),
				qb,
				qb.getRootParameters(),
				revisionPropertyPath,
				verEntCfg.getRevisionEndFieldName(),
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
			qb.getRootParameters().addWhereWithParam( verEntCfg.getRevisionTypePropName(), "<>", RevisionType.DEL );
		}

		// all specified conditions
		for ( AuditCriterion criterion : criterions ) {
			criterion.addToQuery( enversService, versionsReader, entityName, qb, qb.getRootParameters() );
		}

		Query query = buildQuery();
		// add named parameter (only used for ValidAuditTimeStrategy)
		List<String> params = Arrays.asList( query.getNamedParameters() );
		if ( params.contains( REVISION_PARAMETER ) ) {
			query.setParameter( REVISION_PARAMETER, revision );
		}
		List queryResult = query.list();

		if ( hasProjection ) {
			return queryResult;
		}
		else {
			List result = new ArrayList();
			entityInstantiator.addInstancesFromVersionsEntities( entityName, result, queryResult, revision );

			return result;
		}
	}
}
