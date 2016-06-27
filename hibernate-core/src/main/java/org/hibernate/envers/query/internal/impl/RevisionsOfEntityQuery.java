/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.JoinType;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.AuditAssociationQuery;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.proxy.HibernateProxy;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 */
public class RevisionsOfEntityQuery extends AbstractAuditQuery {
	private final boolean selectEntitiesOnly;
	private final boolean selectDeletedEntities;

	public RevisionsOfEntityQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Class<?> cls,
			boolean selectEntitiesOnly,
			boolean selectDeletedEntities) {
		super( enversService, versionsReader, cls );

		this.selectEntitiesOnly = selectEntitiesOnly;
		this.selectDeletedEntities = selectDeletedEntities;
	}

	public RevisionsOfEntityQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Class<?> cls, String entityName,
			boolean selectEntitiesOnly,
			boolean selectDeletedEntities) {
		super( enversService, versionsReader, cls, entityName );

		this.selectEntitiesOnly = selectEntitiesOnly;
		this.selectDeletedEntities = selectDeletedEntities;
	}

	private Number getRevisionNumber(Map versionsEntity) {
		AuditEntitiesConfiguration verEntCfg = enversService.getAuditEntitiesConfiguration();

		String originalId = verEntCfg.getOriginalIdPropName();
		String revisionPropertyName = verEntCfg.getRevisionFieldName();

		Object revisionInfoObject = ( (Map) versionsEntity.get( originalId ) ).get( revisionPropertyName );

		if ( revisionInfoObject instanceof HibernateProxy ) {
			return (Number) ( (HibernateProxy) revisionInfoObject ).getHibernateLazyInitializer().getIdentifier();
		}
		else {
			// Not a proxy - must be read from cache or with a join
			return enversService.getRevisionInfoNumberReader().getRevisionNumber( revisionInfoObject );
		}
	}

	@SuppressWarnings({"unchecked"})
	public List list() throws AuditException {
		AuditEntitiesConfiguration verEntCfg = enversService.getAuditEntitiesConfiguration();

        /*
		The query that should be executed in the versions table:
        SELECT e (unless another projection is specified) FROM ent_ver e, rev_entity r WHERE
          e.revision_type != DEL (if selectDeletedEntities == false) AND
          e.revision = r.revision AND
          (all specified conditions, transformed, on the "e" entity)
          ORDER BY e.revision ASC (unless another order or projection is specified)
         */
		if ( !selectDeletedEntities ) {
			// e.revision_type != DEL AND
			qb.getRootParameters().addWhereWithParam( verEntCfg.getRevisionTypePropName(), "<>", RevisionType.DEL );
		}

		// all specified conditions, transformed
		for ( AuditCriterion criterion : criterions ) {
			criterion.addToQuery(
					enversService,
					versionsReader,
					aliasToEntityNameMap,
					QueryConstants.REFERENCED_ENTITY_ALIAS,
					qb,
					qb.getRootParameters()
			);
		}

		if ( !hasProjection() && !hasOrder ) {
			String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
			qb.addOrder( QueryConstants.REFERENCED_ENTITY_ALIAS, revisionPropertyPath, true );
		}

		if ( !selectEntitiesOnly ) {
			qb.addFrom( enversService.getAuditEntitiesConfiguration().getRevisionInfoEntityName(), "r", true );
			qb.getRootParameters().addWhere(
					enversService.getAuditEntitiesConfiguration().getRevisionNumberPath(),
					true,
					"=",
					"r.id",
					false
			);
		}

		List<Object> queryResult = buildAndExecuteQuery();
		if ( hasProjection() ) {
			return queryResult;
		}
		else {
			List entities = new ArrayList();
			String revisionTypePropertyName = verEntCfg.getRevisionTypePropName();

			for ( Object resultRow : queryResult ) {
				Map versionsEntity;
				Object revisionData;

				if ( selectEntitiesOnly ) {
					versionsEntity = (Map) resultRow;
					revisionData = null;
				}
				else {
					Object[] arrayResultRow = (Object[]) resultRow;
					versionsEntity = (Map) arrayResultRow[0];
					revisionData = arrayResultRow[1];
				}

				Number revision = getRevisionNumber( versionsEntity );

				Object entity = entityInstantiator.createInstanceFromVersionsEntity(
						entityName,
						versionsEntity,
						revision
				);

				if ( !selectEntitiesOnly ) {
					entities.add( new Object[] {entity, revisionData, versionsEntity.get( revisionTypePropertyName )} );
				}
				else {
					entities.add( entity );
				}
			}

			return entities;
		}
	}

	@Override
	public AuditAssociationQuery<? extends AuditQuery> traverseRelation(String associationName, JoinType joinType) {
		throw new UnsupportedOperationException( "Not yet implemented for revisions of entity queries" );
	}

}
